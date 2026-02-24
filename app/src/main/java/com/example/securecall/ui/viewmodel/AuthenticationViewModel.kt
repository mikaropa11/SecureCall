package com.example.securecall.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecall.domain.model.User
import com.example.securecall.domain.model.UserStatus
import com.example.securecall.domain.repository.AuthenticationRepository
import com.google.firebase.auth.FirebaseUser
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

    private val _usernameCheckState = MutableStateFlow<UsernameCheckState>(UsernameCheckState.Idle)
    val usernameCheckState = _usernameCheckState.asStateFlow()

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

    fun signUpWithEmail(email: String, password: String, name: String, username: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authenticationRepository.signUpWithEmail(email, password).fold(
                onSuccess = { firebaseUser ->
                    firebaseUser.sendEmailVerification()
                    val user = User(
                        userId = firebaseUser.uid,
                        name = name,
                        username = username,
                        email = email,
                        photoUrl = null,
                        faceEmbedding = null,
                        status = UserStatus.ONLINE,
                        lastSeen = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis()
                    )

                    authenticationRepository.saveUserProfile(user).fold(
                        onSuccess = {
                            _authState.value = AuthState.AuthenticatedWithoutVerification
                        },
                        onFailure = {
                            _authState.value = AuthState.Error("Error saving profile")
                        }
                    )
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
                    _authState.value = AuthState.AuthenticatedWithoutVerification
                    Log.d("Authentication - loginWithEmail()", "$email registered")
                },
                onFailure = {
                    _authState.value = AuthState.Error(it.message ?: "Login Error")
                    Log.e("Authentication - loginWithEmail()", "Error signing in with this email: $email")
                }
            )
        }
    }

    fun verifyPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authenticationRepository.verifyPhoneCredential(credential).fold(
                onSuccess = {
                    _authState.value = AuthState.Authenticated
                    Log.d("Authentication - verifyPhoneCredential()", "Phone number registered")
                },
                onFailure = {
                    _authState.value = AuthState.Error(it.message ?: "Failed to verify phone number")
                    Log.e("Authentication - verifyPhoneCredential()", "Error verifying phone number")
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

    fun getCurrentUserId(): String? {
        return authenticationRepository.currentUser?.uid
    }

    fun isNewUser(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            authenticationRepository.isNewUser(userId).fold(
                onSuccess = { isNew -> onResult(isNew) },
                onFailure = { onResult(true) }
            )
        }
    }

    fun checkUsernameAvailability(username: String) {
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _usernameCheckState.value = UsernameCheckState.Invalid("Solo letras, números y guión bajo")
            return
        }

        viewModelScope.launch {
            _usernameCheckState.value = UsernameCheckState.Checking
            authenticationRepository.checkUsernameAvailable(username).fold(
                onSuccess = { isAvailable ->
                    _usernameCheckState.value = if (isAvailable) {
                        UsernameCheckState.Available
                    } else {
                        UsernameCheckState.Unavailable
                    }
                },
                onFailure = {
                    _usernameCheckState.value = UsernameCheckState.Error(it.message ?: "Error al verificar")
                }
            )
        }
    }

    fun resetUsernameCheck() {
        _usernameCheckState.value = UsernameCheckState.Idle
    }
    fun saveUserProfile(user: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            authenticationRepository.saveUserProfile(user).fold(
                onSuccess = {
                    _authState.value = AuthState.AuthenticatedWithoutVerification
                    onResult(true)
                },
                onFailure = {
                    onResult(false)
                }
            )
        }
    }

    fun getUserProfile(userId: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            authenticationRepository.getUserProfile(userId).fold(
                onSuccess = { user -> onResult(user) },
                onFailure = { onResult(null) }
            )
        }
    }

    fun updateUserStatus(status: UserStatus) {
        val userId = getCurrentUserId() ?: return  // ← return simple
        viewModelScope.launch {
            authenticationRepository.updateUserStatus(userId, status)
        }
    }

    fun updateFaceEmbedding(embedding: List<Float>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {
                onResult(false)
                return@launch
            }

            authenticationRepository.updateFaceEmbedding(userId, embedding).fold(
                onSuccess = {
                    onResult(true)
                    _authState.value = AuthState.Authenticated
                },
                onFailure = {
                    _authState.value = AuthState.AuthenticatedWithoutVerification
                    onResult(false)
                }
            )
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object AuthenticatedWithoutVerification: AuthState()
    object Unauthenticated : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class UsernameCheckState {
    object Idle : UsernameCheckState()
    object Checking : UsernameCheckState()
    object Available : UsernameCheckState()
    object Unavailable : UsernameCheckState()
    data class Invalid(val reason: String) : UsernameCheckState()
    data class Error(val message: String) : UsernameCheckState()
}
