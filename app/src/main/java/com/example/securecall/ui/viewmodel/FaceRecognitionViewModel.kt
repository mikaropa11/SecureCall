package com.example.securecall.ui.viewmodel

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecall.data.repository.AuthResult
import com.example.securecall.data.repository.RegisterResult
import com.example.securecall.domain.repository.FaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class FaceRecognitionViewModel @Inject constructor(
    private val repository: FaceRepository
) : ViewModel() {

    private val _faceState = MutableStateFlow<FaceState>(FaceState.Detecting)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val faceState: StateFlow<FaceState> = _faceState
    val authState: StateFlow<AuthState> = _authState

    private var isProcessing = false

    fun authenticate(imageProxy: ImageProxy) {

        if (isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true

        viewModelScope.launch {
            try {

                when (repository.authenticate(imageProxy)) {
                    AuthResult.Collecting -> Unit
                    AuthResult.Match -> {
                        _faceState.value = FaceState.Match
                        _authState.value = AuthState.Authenticated
                    }
                    AuthResult.NoMatch -> {
                        _faceState.value = FaceState.NotMatching
                    }
                    AuthResult.NoFace -> {
                        _faceState.value = FaceState.NoFace
                    }
                    is AuthResult.Error -> {
                        _faceState.value = FaceState.Detecting
                    }
                }

            } finally {
                isProcessing = false
            }
        }
    }

    fun register(imageProxy: ImageProxy) {

        if (isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true

        viewModelScope.launch {
            try {

                when (repository.register(imageProxy)) {
                    RegisterResult.CollectingFrames -> Unit
                    is RegisterResult.StepCompleted -> Unit
                    RegisterResult.Saved -> {
                        _faceState.value = FaceState.Registered
                        _authState.value = AuthState.Authenticated
                    }
                    RegisterResult.NoFace -> {
                        _faceState.value = FaceState.NoFace
                    }
                    is RegisterResult.Error -> {
                        _faceState.value = FaceState.Detecting
                    }
                }

            } finally {
                isProcessing = false
            }
        }
    }
}

sealed class FaceState {
    object Detecting : FaceState()
    object NoFace : FaceState()
    object NotMatching : FaceState()
    object Match : FaceState()
    object Registered : FaceState()
}
