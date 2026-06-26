package com.example.clearer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.clearer.data.AuthRepository
import java.util.Arrays
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        loadAuthState()
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun submitSetup(passwordInput: String, confirmPasswordInput: String) {
        val password = passwordInput.toCharArray()
        val confirmPassword = confirmPasswordInput.toCharArray()

        when {
            passwordInput.isBlank() -> {
                clearSecrets(password, confirmPassword)
                showAuthFormError("Enter a password.")
            }

            passwordInput.length < AuthRepository.MIN_PASSWORD_LENGTH -> {
                clearSecrets(password, confirmPassword)
                showAuthFormError("Password must be at least 4 characters.")
            }

            !Arrays.equals(password, confirmPassword) -> {
                clearSecrets(password, confirmPassword)
                showAuthFormError("Passwords do not match.")
            }

            else -> {
                _uiState.update { it.copy(authState = AuthState.Setup, isLoading = true, message = null) }
                viewModelScope.launch {
                    val result = authRepository.setPassword(password)
                    clearSecrets(password, confirmPassword)

                    result.fold(
                        onSuccess = {
                            _uiState.value = AuthUiState(authState = AuthState.Unlocked)
                        },
                        onFailure = {
                            _uiState.update { state ->
                                state.copy(
                                    authState = AuthState.Setup,
                                    isLoading = false,
                                    message = "Unable to save the password on this device.",
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    fun submitUnlock(passwordInput: String) {
        val password = passwordInput.toCharArray()
        if (passwordInput.isBlank()) {
            clearSecrets(password)
            showAuthFormError("Enter your password.")
            return
        }

        _uiState.update { it.copy(authState = AuthState.Unlock, isLoading = true, message = null) }
        viewModelScope.launch {
            val verified = authRepository.verifyPassword(password)
            clearSecrets(password)

            _uiState.update { state ->
                if (verified) {
                    AuthUiState(authState = AuthState.Unlocked)
                } else {
                    state.copy(
                        authState = AuthState.Unlock,
                        isLoading = false,
                        message = "Password incorrect.",
                    )
                }
            }
        }
    }

    private fun loadAuthState() {
        viewModelScope.launch {
            val authState = authRepository.loadAuthState()
                .fold(
                    onSuccess = { hasPassword ->
                        if (hasPassword) AuthState.Unlock else AuthState.Setup
                    },
                    onFailure = {
                        AuthState.Error("Unable to access secure password storage on this device.")
                    },
                )

            _uiState.value = AuthUiState(authState = authState)
        }
    }

    private fun showAuthFormError(message: String) {
        _uiState.update { state ->
            state.copy(
                authState = if (state.authState == AuthState.Unlock) AuthState.Unlock else AuthState.Setup,
                isLoading = false,
                message = message,
            )
        }
    }

    private fun clearSecrets(vararg values: CharArray) {
        values.forEach { chars -> chars.fill('\u0000') }
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository) as T
                }
            }
    }
}

data class AuthUiState(
    val authState: AuthState = AuthState.Loading,
    val isLoading: Boolean = false,
    val message: String? = null,
)

sealed interface AuthState {
    data object Loading : AuthState
    data object Setup : AuthState
    data object Unlock : AuthState
    data object Unlocked : AuthState
    data class Error(val message: String) : AuthState
}
