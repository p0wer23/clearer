package com.example.clearer.storage

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.math.min

private const val BYTES_PER_GB = 1024L * 1024L * 1024L
private const val MIN_FREE_SPACE_BYTES = BYTES_PER_GB
private const val FILE_SIZE_LIMIT_BYTES = 256L * 1024L * 1024L
private const val CHUNK_SIZE_BYTES = 1024 * 1024

class FillerRepository(context: Context) {
    private val outputDirectory: File =
        context.getExternalFilesDir("filler")
            ?: File(context.filesDir, "filler")

    val outputDirectoryPath: String = outputDirectory.absolutePath

    fun availableWritableBytes(): Long {
        ensureOutputDirectory()
        return (outputDirectory.usableSpace - MIN_FREE_SPACE_BYTES).coerceAtLeast(0L)
    }

    suspend fun fill(
        targetBytes: Long,
        onProgress: suspend (FillerProgress) -> Unit,
    ): FillerResult {
        var bytesWritten = 0L
        return runCatching {
            ensureOutputDirectory()

            val random = SecureRandom()
            val buffer = ByteArray(CHUNK_SIZE_BYTES)
            val timestamp = FILE_NAME_FORMAT.format(Date())
            var fileIndex = 1

            while (bytesWritten < targetBytes) {
                coroutineContext.ensureActive()

                val file = File(outputDirectory, buildFileName(timestamp, fileIndex))
                var fileBytesWritten = 0L

                FileOutputStream(file).use { output ->
                    while (bytesWritten < targetBytes && fileBytesWritten < FILE_SIZE_LIMIT_BYTES) {
                        coroutineContext.ensureActive()

                        val freeBytes = outputDirectory.usableSpace
                        val bytesAboveBuffer = (freeBytes - MIN_FREE_SPACE_BYTES).coerceAtLeast(0L)
                        if (bytesAboveBuffer <= 0L) {
                            return FillerResult.StoppedLowSpace(
                                bytesWritten = bytesWritten,
                                directoryPath = outputDirectoryPath,
                            )
                        }

                        val remainingTargetBytes = targetBytes - bytesWritten
                        val remainingFileBytes = FILE_SIZE_LIMIT_BYTES - fileBytesWritten
                        val nextChunkSize = min(
                            remainingTargetBytes,
                            min(remainingFileBytes, min(CHUNK_SIZE_BYTES.toLong(), bytesAboveBuffer)),
                        ).toInt()

                        if (nextChunkSize <= 0 || freeBytes - nextChunkSize < MIN_FREE_SPACE_BYTES) {
                            return FillerResult.StoppedLowSpace(
                                bytesWritten = bytesWritten,
                                directoryPath = outputDirectoryPath,
                            )
                        }

                        random.nextBytes(buffer)
                        output.write(buffer, 0, nextChunkSize)
                        output.fd.sync()

                        bytesWritten += nextChunkSize
                        fileBytesWritten += nextChunkSize

                        onProgress(
                            FillerProgress(
                                bytesWritten = bytesWritten,
                                targetBytes = targetBytes,
                                currentFileName = file.name,
                                freeBytes = (freeBytes - nextChunkSize).coerceAtLeast(0L),
                            ),
                        )
                    }
                }

                fileIndex += 1
            }

            FillerResult.Completed(
                bytesWritten = bytesWritten,
                directoryPath = outputDirectoryPath,
            )
        }.getOrElse { throwable ->
            if (throwable is kotlinx.coroutines.CancellationException) {
                throw throwable
            }

            FillerResult.Failed(
                bytesWritten = bytesWritten,
                message = throwable.userMessage(),
                directoryPath = outputDirectoryPath,
            )
        }
    }

    private fun ensureOutputDirectory() {
        if (outputDirectory.exists()) {
            check(outputDirectory.isDirectory) { "Filler output path is not a directory." }
            return
        }

        check(outputDirectory.mkdirs()) { "Unable to create filler output directory." }
    }

    private fun buildFileName(timestamp: String, fileIndex: Int): String {
        return "clearer-fill-$timestamp-${fileIndex.toString().padStart(3, '0')}.bin"
    }

    private fun Throwable.userMessage(): String {
        return when (this) {
            is IOException -> "Write failed while generating filler data."
            else -> message ?: "Unable to generate filler data."
        }
    }

    companion object {
        private val FILE_NAME_FORMAT = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    }
}

data class FillerProgress(
    val bytesWritten: Long,
    val targetBytes: Long,
    val currentFileName: String?,
    val freeBytes: Long,
)

sealed interface FillerResult {
    data class Completed(
        val bytesWritten: Long,
        val directoryPath: String,
    ) : FillerResult

    data class StoppedLowSpace(
        val bytesWritten: Long,
        val directoryPath: String,
    ) : FillerResult

    data class Failed(
        val bytesWritten: Long,
        val message: String,
        val directoryPath: String,
    ) : FillerResult
}
