package com.example.securecall.ui.viewmodel

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecall.data.repository.FaceResult
import com.example.securecall.domain.repository.AuthenticationRepository
import com.example.securecall.domain.repository.FaceRepository
import com.example.securecall.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class FaceRecognitionViewModel @Inject constructor(
    private val repository: FaceRepository,
    private val userRepository: UserRepository
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

                val result = repository.getEmbedding(imageProxy)

                when (result) {

                    is FaceResult.Success -> {
                        repository.addEmbedding(result.embedding)

                        if (repository.hasEnoughFrames()) {
                            val finalEmbedding = repository.getAveragedEmbedding()
                            repository.resetEmbeddings()
                            var match = false
                            finalEmbedding?.let {
                                match = repository.compareEmbedding(it)
                            }
                            if (match) {
                                _faceState.value = FaceState.Match
                                _authState.value = AuthState.Authenticated
                            } else {
                                _faceState.value = FaceState.NotMatching
                            }

                        }
                    }

                    FaceResult.NoFace -> {
                        _faceState.value = FaceState.NoFace
                    }

                    FaceResult.Error -> {
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

                val result = repository.getEmbedding(imageProxy)

                when (result) {

                    is FaceResult.Success -> {
                        repository.addEmbedding(result.embedding)

                        if (repository.hasEnoughFrames()) {
                            val finalEmbedding = repository.getAveragedEmbedding()
                            repository.resetEmbeddings()

                            userRepository.saveEmbedding(finalEmbedding)
                            _faceState.value = FaceState.Registered
                            _authState.value = AuthState.Authenticated
                        }
                    }

                    FaceResult.NoFace -> {
                        _faceState.value = FaceState.NoFace
                    }

                    FaceResult.Error -> {
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