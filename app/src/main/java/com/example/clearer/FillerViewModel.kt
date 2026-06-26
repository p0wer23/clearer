package com.example.clearer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.clearer.storage.FillerRepository
import com.example.clearer.storage.FillerResult
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val BYTES_PER_GB = BigDecimal(1024L * 1024L * 1024L)

class FillerViewModel(
    private val fillerRepository: FillerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        FillerUiState(
            outputDirectoryPath = fillerRepository.outputDirectoryPath,
            statusMessage = "Ready to generate filler files.",
        ),
    )
    val uiState: StateFlow<FillerUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    fun updateTargetGbInput(value: String) {
        _uiState.update { state ->
            state.copy(
                targetGbInput = value,
                errorMessage = null,
                statusMessage = buildStatusMessage(
                    input = value,
                    defaultMessage = if (state.isRunning) state.statusMessage else "Ready to generate filler files.",
                ),
            )
        }
    }

    fun start() {
        if (activeJob?.isActive == true) {
            return
        }

        val currentInput = uiState.value.targetGbInput
        val targetBytes = parseTargetBytes(currentInput)
            ?: return showError("Enter a valid positive GB amount.")

        val availableWritableBytes = loadAvailableWritableBytes()
            ?: return showError("Unable to access filler storage on this device.")
        if (availableWritableBytes <= 0L) {
            return showError("Not enough free space to start. Clear at least 1 GB beyond the safety buffer.")
        }

        _uiState.update { state ->
            state.copy(
                isRunning = true,
                bytesWritten = 0L,
                targetBytes = targetBytes,
                currentFileName = null,
                errorMessage = null,
                statusMessage = if (targetBytes > availableWritableBytes) {
                    "Requested size exceeds writable space. Fill may stop early at the 1 GB safety buffer."
                } else {
                    "Generating filler files..."
                },
            )
        }

        activeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = fillerRepository.fill(targetBytes = targetBytes) { progress ->
                    _uiState.update { state ->
                        state.copy(
                            bytesWritten = progress.bytesWritten,
                            targetBytes = progress.targetBytes,
                            currentFileName = progress.currentFileName,
                            statusMessage = "Generating filler files...",
                        )
                    }
                }

                _uiState.update { state ->
                    when (result) {
                        is FillerResult.Completed -> state.copy(
                            isRunning = false,
                            bytesWritten = result.bytesWritten,
                            currentFileName = null,
                            statusMessage = "Fill complete. Delete generated files manually outside the app.",
                        )

                        is FillerResult.StoppedLowSpace -> state.copy(
                            isRunning = false,
                            bytesWritten = result.bytesWritten,
                            currentFileName = null,
                            statusMessage = "Stopped at the 1 GB safety buffer. Delete generated files manually outside the app.",
                        )

                        is FillerResult.Failed -> state.copy(
                            isRunning = false,
                            bytesWritten = result.bytesWritten,
                            currentFileName = null,
                            errorMessage = result.message,
                            statusMessage = "Generation stopped.",
                        )
                    }
                }
            } catch (exception: CancellationException) {
                _uiState.update { state ->
                    state.copy(
                        isRunning = false,
                        currentFileName = null,
                        statusMessage = "Fill canceled. Partial files remain for manual deletion.",
                    )
                }
                throw exception
            } finally {
                activeJob = null
            }
        }
    }

    fun cancel() {
        activeJob?.cancel()
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        activeJob?.cancel()
        super.onCleared()
    }

    private fun showError(message: String) {
        _uiState.update { state ->
            state.copy(
                errorMessage = message,
                statusMessage = buildStatusMessage(
                    input = state.targetGbInput,
                    defaultMessage = state.statusMessage,
                ),
            )
        }
    }

    private fun buildStatusMessage(input: String, defaultMessage: String): String {
        val targetBytes = parseTargetBytes(input) ?: return defaultMessage
        val availableWritableBytes = loadAvailableWritableBytes() ?: return defaultMessage
        return if (targetBytes > availableWritableBytes && availableWritableBytes > 0L) {
            "This request may stop early when free space reaches the 1 GB safety buffer."
        } else {
            defaultMessage
        }
    }

    private fun loadAvailableWritableBytes(): Long? {
        return runCatching { fillerRepository.availableWritableBytes() }.getOrNull()
    }

    private fun parseTargetBytes(input: String): Long? {
        val normalized = input.trim()
        if (normalized.isEmpty()) {
            return null
        }

        return runCatching {
            val gigabytes = BigDecimal(normalized)
            if (gigabytes <= BigDecimal.ZERO) {
                return null
            }

            gigabytes
                .multiply(BYTES_PER_GB)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
                .takeIf { it > 0L }
        }.getOrNull()
    }

    companion object {
        fun factory(fillerRepository: FillerRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FillerViewModel(fillerRepository) as T
                }
            }
    }
}

data class FillerUiState(
    val targetGbInput: String = "",
    val isRunning: Boolean = false,
    val bytesWritten: Long = 0L,
    val targetBytes: Long = 0L,
    val currentFileName: String? = null,
    val outputDirectoryPath: String = "",
    val statusMessage: String = "",
    val errorMessage: String? = null,
)
