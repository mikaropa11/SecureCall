package com.example.securecall.ui.view.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecall.ui.components.AuthTextField
import com.example.securecall.ui.components.PrimaryButton
import com.example.securecall.ui.components.SecondaryButton
import com.example.securecall.ui.viewmodel.AuthState
import com.example.securecall.ui.viewmodel.AuthenticationViewModel
import com.example.securecall.R

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: AuthenticationViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()

    //Error messages
    val errorRequiredEmail = stringResource(R.string.error_email_required)
    val errorInvalidEmail = stringResource(R.string.error_email_invalid)
    val errorRequiredPassword = stringResource(R.string.error_password_required)
    val errorPasswordMinLength = stringResource(R.string.error_password_min_length)

    // Navegar cuando se autentica
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigateToHome()
        }
    }

    // Mostrar errores
    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            val error = (authState as AuthState.Error).message
            when {
                error.contains("email", ignoreCase = true) -> emailError = error
                error.contains("password", ignoreCase = true) -> passwordError = error
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo/Título
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Email
            AuthTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = stringResource(R.string.email_label),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                isError = emailError != null,
                errorMessage = emailError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            AuthTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = stringResource(R.string.password_label),
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.loginWithEmail(email, password)
                    }
                },
                isError = passwordError != null,
                errorMessage = passwordError
            )

            // Forgot password
            Text(
                text = stringResource(R.string.forgot_password),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
                    .clickable { onNavigateToForgotPassword() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón Login
            PrimaryButton(
                text = stringResource(R.string.login_button),
                onClick = {
                    emailError = null
                    passwordError = null

                    // Basic validations
                    when {
                        email.isBlank() -> emailError = errorRequiredEmail
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                            emailError = errorInvalidEmail
                        password.isBlank() -> passwordError = errorRequiredPassword
                        password.length < 6 -> passwordError = errorPasswordMinLength
                        else -> viewModel.loginWithEmail(email, password)
                    }
                },
                isLoading = authState is AuthState.Loading,
                enabled = email.isNotBlank() && password.isNotBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "O",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Phone
            SecondaryButton(
                text = stringResource(R.string.login_with_phone),
                onClick = { /* TODO: Implementar phone auth */ }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Register
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.no_account) + " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.register_link),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }

        // Error Snackbar
        if (authState is AuthState.Error) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { /* Dismiss */ }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            ) {
                Text((authState as AuthState.Error).message)
            }
        }
    }
}