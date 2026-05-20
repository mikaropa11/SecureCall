package com.example.securecall.ui.view.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecall.ui.viewmodel.AuthenticationViewModel

@Composable
fun LaunchScreen (
    viewModel: AuthenticationViewModel = hiltViewModel(),
    onNavigateToFaceRecognition: () -> Unit,
    onNavigateToSignUp: () -> Unit
){

    val currentUser = viewModel.getCurrentUserId()
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onNavigateToFaceRecognition()
        } else {
            onNavigateToSignUp()
        }
    }
}