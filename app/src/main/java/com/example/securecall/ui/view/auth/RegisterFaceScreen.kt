package com.example.securecall.ui.view.auth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.example.securecall.R
import com.example.securecall.domain.repository.DetectedOrientation
import com.example.securecall.domain.repository.RegistrationProgress
import com.example.securecall.domain.repository.RegistrationStep
import com.example.securecall.ui.viewmodel.HapticEvent
import com.example.securecall.ui.viewmodel.RegisterFaceViewModel
import com.example.securecall.ui.viewmodel.RegisterState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun RegisterFaceScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: RegisterFaceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val state by viewModel.state.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val borderColor = registrationBorderColor(state)

    LaunchedEffect(Unit) {
        viewModel.hapticEvents.collect { event ->
            when (event) {
                HapticEvent.ValidFrame -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                HapticEvent.StepCompleted -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                HapticEvent.RegistrationCompleted -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
    }

    LaunchedEffect(state) {
        if (state is RegisterState.AllStepsCompleted) {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            onNavigateToHome()
        }
    }

    RegisterCameraPermissionEffect(
        context = context,
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        onFrame = viewModel::processFrame
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RegistrationStepIndicator(progress = progress)

        OutlinedCard(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(3.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
        ) {
            Box {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(220.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = borderColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
                            )
                        }
                )

                OrientationArrow(state = state, progress = progress)
            }
        }

        AnimatedInstruction(state = state, progress = progress)
        RegistrationProgress(progress = progress)

        Text(
            text = stringResource(R.string.register_face_later),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onNavigateToLogin() }
                .padding(16.dp)
        )
    }
}

@Composable
private fun RegisterCameraPermissionEffect(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onFrame: (ImageProxy) -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRegisterCamera(context, lifecycleOwner, previewView, onFrame)
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startRegisterCamera(context, lifecycleOwner, previewView, onFrame)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

private fun startRegisterCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onFrame: (ImageProxy) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalyzer.setAnalyzer(executor, onFrame)
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

@Composable
private fun RegistrationStepIndicator(progress: RegistrationProgress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RegistrationStep.entries.forEach { step ->
            val completed = when (step) {
                RegistrationStep.FRONTAL -> progress.frontalCompleted
                RegistrationStep.LEFT -> progress.leftCompleted
                RegistrationStep.RIGHT -> progress.rightCompleted
            }
            val active = step == progress.currentStep && !completed
            val alpha by rememberInfiniteTransition(label = "step-pulse").animateFloat(
                initialValue = if (active) 0.45f else 1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
                label = "step-alpha"
            )
            val color = when {
                completed -> Color.Green
                active -> Color(0xFF2196F3)
                else -> MaterialTheme.colorScheme.outline
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (completed) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OrientationArrow(state: RegisterState, progress: RegistrationProgress) {
    val orientation = when (state) {
        is RegisterState.OrientationError -> state.expected
        else -> progress.currentStep.toOrientation()
    }
    val rotation = when (orientation) {
        DetectedOrientation.LEFT -> 180f
        DetectedOrientation.RIGHT -> 0f
        else -> return
    }
    val alpha by rememberInfiniteTransition(label = "orientation-arrow").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "arrow-alpha"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .rotate(rotation)
                .alpha(alpha),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AnimatedInstruction(state: RegisterState, progress: RegistrationProgress) {
    AnimatedContent(
        targetState = instructionText(state, progress),
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
        label = "register-instruction"
    ) { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun instructionText(state: RegisterState, progress: RegistrationProgress): String {
    return when (state) {
        RegisterState.NoFace -> stringResource(R.string.face_place_inside_frame)
        is RegisterState.OrientationError -> stringResource(instructionRes(state.expected))
        is RegisterState.StepCompleted -> stringResource(R.string.register_step_completed)
        RegisterState.AllStepsCompleted -> stringResource(R.string.face_registered)
        is RegisterState.Error -> state.message
        else -> stringResource(instructionRes(progress.currentStep.toOrientation()))
    }
}

@Composable
private fun RegistrationProgress(progress: RegistrationProgress) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = progress.framesInStep.toFloat() / progress.framesNeeded.toFloat(),
            modifier = Modifier.size(80.dp)
        )
        Text(
            text = "${progress.framesInStep}/${progress.framesNeeded}",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun registrationBorderColor(state: RegisterState): Color {
    return when (state) {
        RegisterState.NoFace -> MaterialTheme.colorScheme.outline
        is RegisterState.OrientationError -> Color.Yellow
        is RegisterState.StepCompleted,
        RegisterState.AllStepsCompleted -> Color.Green
        else -> Color(0xFF2196F3)
    }
}

private fun RegistrationStep.toOrientation(): DetectedOrientation {
    return when (this) {
        RegistrationStep.FRONTAL -> DetectedOrientation.FRONTAL
        RegistrationStep.LEFT -> DetectedOrientation.LEFT
        RegistrationStep.RIGHT -> DetectedOrientation.RIGHT
    }
}

private fun instructionRes(orientation: DetectedOrientation): Int {
    return when (orientation) {
        DetectedOrientation.FRONTAL -> R.string.register_step_frontal
        DetectedOrientation.LEFT -> R.string.register_step_left
        DetectedOrientation.RIGHT -> R.string.register_step_right
        DetectedOrientation.UNKNOWN -> R.string.face_detecting
    }
}
