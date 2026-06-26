package com.example.clearer.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AuthRepository(context: Context) {
    private val preferencesResult: Result<SharedPreferences> = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun loadAuthState(): Result<Boolean> = preferencesResult.map { preferences ->
        val hasHash = preferences.contains(KEY_PASSWORD_HASH)
        val hasSalt = preferences.contains(KEY_PASSWORD_SALT)
        val hasIterations = preferences.contains(KEY_PASSWORD_ITERATIONS)

        when {
            hasHash && hasSalt && hasIterations -> true
            !hasHash && !hasSalt && !hasIterations -> false
            else -> error("Secure password storage is incomplete.")
        }
    }

    fun hasPassword(): Boolean = loadAuthState().getOrDefault(false)

    fun setPassword(password: CharArray): Result<Unit> = runCatching {
        val preferences = preferencesResult.getOrThrow()
        val salt = ByteArray(SALT_LENGTH_BYTES).also(secureRandom::nextBytes)
        val hash = derivePasswordHash(password, salt, PBKDF2_ITERATIONS)

        val saved = preferences.edit()
            .putString(KEY_PASSWORD_HASH, hash.toBase64())
            .putString(KEY_PASSWORD_SALT, salt.toBase64())
            .putInt(KEY_PASSWORD_ITERATIONS, PBKDF2_ITERATIONS)
            .commit()

        check(saved) { "Unable to persist password data." }
    }

    fun verifyPassword(password: CharArray): Boolean {
        val preferences = preferencesResult.getOrElse { return false }
        val storedHash = preferences.getString(KEY_PASSWORD_HASH, null)?.fromBase64() ?: return false
        val salt = preferences.getString(KEY_PASSWORD_SALT, null)?.fromBase64() ?: return false
        val iterations = preferences.getInt(KEY_PASSWORD_ITERATIONS, -1)
        if (iterations <= 0) {
            return false
        }

        val derivedHash = derivePasswordHash(password, salt, iterations)
        return MessageDigest.isEqual(storedHash, derivedHash)
    }

    private fun derivePasswordHash(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val keySpec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return try {
            secretKeyFactory.generateSecret(keySpec).encoded
        } finally {
            keySpec.clearPassword()
        }
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        const val MIN_PASSWORD_LENGTH = 4

        private const val PREFERENCES_NAME = "clearer_auth"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_PASSWORD_ITERATIONS = "password_iterations"
        private const val SALT_LENGTH_BYTES = 32
        private const val PBKDF2_ITERATIONS = 210_000
        private const val KEY_LENGTH_BITS = 256

        private val secureRandom = SecureRandom()
        private val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    }
}
