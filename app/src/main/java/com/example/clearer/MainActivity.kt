package com.example.clearer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clearer.data.AuthRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val authRepository = remember { AuthRepository(applicationContext) }
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.factory(authRepository),
            )

            ClearerApp(authViewModel = authViewModel)
        }
    }
}

@Composable
private fun ClearerApp(authViewModel: AuthViewModel) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val authState = uiState.authState) {
                AuthState.Loading -> LoadingScreen()
                is AuthState.Error -> MessageScreen(
                    title = "Clearer",
                    message = authState.message,
                )
                AuthState.Setup -> SetupScreen(
                    message = uiState.message,
                    isLoading = uiState.isLoading,
                    onDismissMessage = authViewModel::dismissMessage,
                    onSubmit = authViewModel::submitSetup,
                )
                AuthState.Unlock -> UnlockScreen(
                    message = uiState.message,
                    isLoading = uiState.isLoading,
                    onDismissMessage = authViewModel::dismissMessage,
                    onSubmit = authViewModel::submitUnlock,
                )
                AuthState.Unlocked -> UnlockedScreen()
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Preparing secure access...")
    }
}

@Composable
private fun SetupScreen(
    message: String?,
    isLoading: Boolean,
    onDismissMessage: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AuthScaffold(
        title = "Set password",
        body = "Create the local password required before Clearer can be used.",
        message = message,
    ) {
        PasswordField(
            value = password,
            label = "Password",
            enabled = !isLoading,
            imeAction = ImeAction.Next,
            onValueChange = {
                if (message != null) onDismissMessage()
                password = it
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        PasswordField(
            value = confirmPassword,
            label = "Confirm password",
            enabled = !isLoading,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSubmit(password, confirmPassword)
                },
            ),
            onValueChange = {
                if (message != null) onDismissMessage()
                confirmPassword = it
            },
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                focusManager.clearFocus()
                onSubmit(password, confirmPassword)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLoading) "Saving..." else "Set password")
        }
    }
}

@Composable
private fun UnlockScreen(
    message: String?,
    isLoading: Boolean,
    onDismissMessage: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AuthScaffold(
        title = "Unlock Clearer",
        body = "Enter the local password to open the app.",
        message = message,
    ) {
        PasswordField(
            value = password,
            label = "Password",
            enabled = !isLoading,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSubmit(password)
                },
            ),
            onValueChange = {
                if (message != null) onDismissMessage()
                password = it
            },
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                focusManager.clearFocus()
                onSubmit(password)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLoading) "Checking..." else "Unlock")
        }
    }
}

@Composable
private fun AuthScaffold(
    title: String,
    body: String,
    message: String?,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        content()
    }
}

@Composable
private fun PasswordField(
    value: String,
    label: String,
    enabled: Boolean,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
    )
}

@Composable
private fun UnlockedScreen() {
    MessageScreen(
        title = "Clearer",
        message = "Password protection is active. Storage filler controls are not implemented yet.",
    )
}

@Composable
private fun MessageScreen(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
