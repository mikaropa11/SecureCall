package com.example.securecall.ui.viewmodel

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecall.data.repository.RegisterResult
import com.example.securecall.domain.repository.DetectedOrientation
import com.example.securecall.domain.repository.FaceRepository
import com.example.securecall.domain.repository.RegistrationProgress
import com.example.securecall.domain.repository.RegistrationStep
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterFaceViewModel @Inject constructor(
    private val repository: FaceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RegisterState>(RegisterState.Detecting)
    val state: StateFlow<RegisterState> = _state

    private val _progress = MutableStateFlow(repository.getRegistrationProgressPublic())
    val progress: StateFlow<RegistrationProgress> = _progress

    private val _hapticEvents = MutableSharedFlow<HapticEvent>(extraBufferCapacity = 1)
    val hapticEvents: SharedFlow<HapticEvent> = _hapticEvents

    private var isProcessing = false

    fun processFrame(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true
        viewModelScope.launch {
            try {
                val result = repository.register(imageProxy)
                _progress.value = repository.getRegistrationProgressPublic()
                when (result) {
                    RegisterResult.CollectingFrames -> Unit
                    is RegisterResult.StepCompleted -> {
                        _hapticEvents.tryEmit(HapticEvent.StepCompleted)
                        _state.value = RegisterState.StepCompleted(result.step)
                        delay(STEP_TRANSITION_DELAY_MS)
                        _state.value = RegisterState.Detecting
                    }
                    RegisterResult.Saved -> {
                        _hapticEvents.tryEmit(HapticEvent.RegistrationCompleted)
                        _state.value = RegisterState.AllStepsCompleted
                    }
                    RegisterResult.NoFace -> _state.value = RegisterState.NoFace
                    is RegisterResult.Error -> _state.value = RegisterState.Error(result.cause)
                }
            } finally {
                isProcessing = false
            }
        }
    }

    companion object {
        private const val STEP_TRANSITION_DELAY_MS = 800L
    }
}

sealed class RegisterState {
    data object Detecting : RegisterState()
    data object NoFace : RegisterState()
    data class StepInProgress(val step: RegistrationStep) : RegisterState()
    data class StepCompleted(val step: RegistrationStep) : RegisterState()
    data class OrientationError(
        val expected: DetectedOrientation,
        val actual: DetectedOrientation
    ) : RegisterState()
    data object AllStepsCompleted : RegisterState()
    data class Error(val message: String) : RegisterState()
}

enum class HapticEvent {
    ValidFrame,
    StepCompleted,
    RegistrationCompleted
}
