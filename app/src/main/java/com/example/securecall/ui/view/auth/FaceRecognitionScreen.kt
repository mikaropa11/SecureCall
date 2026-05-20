package com.example.securecall.ui.view.auth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock.sleep
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.example.securecall.ui.viewmodel.FaceRecognitionViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.securecall.ui.viewmodel.AuthState
import com.example.securecall.ui.viewmodel.FaceState
import com.example.securecall.R


@Composable
fun FaceRecognitionScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: FaceRecognitionViewModel = hiltViewModel(),
    mode: FaceMode
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val faceState by viewModel.faceState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    val borderColor = when (faceState) {
        FaceState.Match -> Color.Green
        FaceState.Registered -> Color.Green
        FaceState.NotMatching -> MaterialTheme.colorScheme.error
        FaceState.Detecting -> Color.Yellow
        FaceState.NoFace -> MaterialTheme.colorScheme.outline
    }

    when (authState) {
        AuthState.Authenticated -> {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                cameraProviderFuture.get().unbindAll()
                onNavigateToHome()
            }, ContextCompat.getMainExecutor(context))
        }
        else -> {}
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera(context, lifecycleOwner, previewView, viewModel, mode)
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startCamera(context, lifecycleOwner, previewView, viewModel, mode)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(32.dp))

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
                            val strokeWidth = 4.dp.toPx()
                            drawRoundRect(
                                color = borderColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
                            )
                        }
                )
            }
        }

        Text(
            text = when (faceState) {
                FaceState.Match -> stringResource(R.string.face_access_granted)
                FaceState.Registered -> stringResource(R.string.face_registered)
                FaceState.NotMatching -> stringResource(R.string.face_not_matching)
                FaceState.Detecting -> stringResource(R.string.face_detecting)
                FaceState.NoFace -> stringResource(R.string.face_place_inside_frame)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        val text = if (mode == FaceMode.REGISTRY) {
            stringResource(R.string.access_without_verification)
        } else {
            stringResource(R.string.access_with_password)
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onNavigateToLogin() }
                .padding(16.dp)
        )
    }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    viewModel: FaceRecognitionViewModel,
    mode: FaceMode
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

        sleep(1000)
        imageAnalyzer.setAnalyzer(executor) { imageProxy ->
            when (mode) {

                FaceMode.AUTH -> {
                    viewModel.authenticate(imageProxy)
                }

                FaceMode.REGISTRY -> {
                    viewModel.register(imageProxy)
                }
            }
        }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }, ContextCompat.getMainExecutor(context))
}

enum class FaceMode {
    AUTH,
    REGISTRY
}