package com.example.securecall.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecall.domain.repository.AuthenticationRepository
import com.google.firebase.auth.PhoneAuthCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
): ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    init {
        isUserAuthenticated()
    }

    private fun isUserAuthenticated() {
        if(authenticationRepository.isUserAuthenticated()) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authenticationRepository.signUpWithEmail(email, password).fold(
                onSuccess = {
                    _authState.value = AuthState.Authenticated
                    Log.d("Authentication - signUpWithEmail()", "$email registered")
                },
                onFailure = {
                    _authState.value = AuthState.Error(it.message ?: "SignUp Error")
                    Log.e("Authentication - signUpWithEmail()", "Error signing up this email: $email")
                }
            )
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authenticationRepository.loginWithEmail(email, password).fold(
                onSuccess = {
                    _authState.value = AuthState.Authenticated
                    Log.d("Authentication - loginWithEmail()", "$email registered")
                },
                onFailure = {
                    _authState.value = AuthState.Error(it.message ?: "Login Error")
                    Log.e("Authentication - loginWithEmail()", "Error signing in with this email: $email")
                }
            )
        }
    }

    fun verifyPhoneCredential(credential: PhoneAuthCredential, phoneNumber: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authenticationRepository.verifyPhoneCredential(credential).fold(
                onSuccess = {
                    _authState.value = AuthState.Authenticated
                    Log.d("Authentication - verifyPhoneCredential()", "$phoneNumber registered")
                },
                onFailure = {
                    _authState.value = AuthState.Error(it.message ?: "Failed to verify phone number")
                    Log.e("Authentication - verifyPhoneCredential()", "Error verifying this number: $phoneNumber")
                }
            )
        }
    }

    fun recoverPassword(email: String) {
        viewModelScope.launch {
            authenticationRepository.recoverPassword(email).fold(
                onSuccess = {
                    _authState.value = AuthState.PasswordResetSent
                    Log.d("Authentication - recoverPassword()", "Email sent to $email")
                },
                onFailure = {
                    _authState.value = AuthState.Error(it.message ?: "Password reset failed")
                    Log.d("Authentication - recoverPassword()", "Failed to send password reset email to $email")
                }
            )
        }
    }

    fun logOut() {
        authenticationRepository.logOut()
        _authState.value = AuthState.Unauthenticated
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
}