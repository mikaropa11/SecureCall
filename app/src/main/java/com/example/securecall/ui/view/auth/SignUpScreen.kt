package com.example.securecall.ui.view.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecall.R
import com.example.securecall.ui.components.AuthTextField
import com.example.securecall.ui.components.PrimaryButton
import com.example.securecall.ui.components.SecondaryButton
import com.example.securecall.ui.viewmodel.AuthState
import com.example.securecall.ui.viewmodel.AuthenticationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthenticationViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()

    //Error messages
    val errorNameRequired = stringResource(R.string.error_name_required)
    val errorNameMinLength = stringResource(R.string.error_name_min_length)
    val errorEmailRequired = stringResource(R.string.error_email_required)
    val errorEmailInvalid = stringResource(R.string.error_email_invalid)
    val errorPasswordRequired = stringResource(R.string.error_password_required)
    val errorPasswordMinLength = stringResource(R.string.error_password_min_length)
    val errorConfirmPassword = stringResource(R.string.error_confirm_password)
    val errorPasswordDontMatch = stringResource(R.string.error_passwords_dont_match)

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigateToHome()
        }
    }

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
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Botón volver
        IconButton(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Nombre
            AuthTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = stringResource(R.string.name_label),
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                isError = nameError != null,
                errorMessage = nameError
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                imeAction = ImeAction.Next,
                isError = passwordError != null,
                errorMessage = passwordError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            AuthTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = null
                },
                label = stringResource(R.string.confirm_password_label),
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = {
                    // Validar y registrar
                },
                isError = confirmPasswordError != null,
                errorMessage = confirmPasswordError
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón Registrar
            PrimaryButton(
                text = stringResource(R.string.register_button),
                onClick = {
                    nameError = null
                    emailError = null
                    passwordError = null
                    confirmPasswordError = null

                    when {
                        name.isBlank() -> nameError = errorNameRequired
                        name.length < 3 -> nameError = errorNameMinLength
                        email.isBlank() -> emailError = errorEmailRequired
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                            emailError = errorEmailInvalid
                        password.isBlank() -> passwordError = errorPasswordRequired
                        password.length < 6 -> passwordError = errorPasswordMinLength
                        confirmPassword.isBlank() -> confirmPasswordError = errorConfirmPassword
                        password != confirmPassword -> confirmPasswordError = errorPasswordDontMatch
                        else -> viewModel.signUpWithEmail(email, password)
                    }
                },
                isLoading = authState is AuthState.Loading,
                enabled = name.isNotBlank() && email.isNotBlank() &&
                        password.isNotBlank() && confirmPassword.isNotBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            SecondaryButton(
                text = stringResource(R.string.register_with_phone),
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.have_account) + " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.login_link),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

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