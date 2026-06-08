package com.example.securecall.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.securecall.domain.model.CallType
import com.example.securecall.ui.view.HomeScreen
import com.example.securecall.ui.view.auth.FaceRecognitionScreen
import com.example.securecall.ui.view.auth.FaceMode
import com.example.securecall.ui.view.auth.LaunchScreen
import com.example.securecall.ui.view.auth.LoginScreen
import com.example.securecall.ui.view.auth.RegisterFaceScreen
import com.example.securecall.ui.view.auth.SignUpScreen
import com.example.securecall.ui.view.call.CallScreen
import com.example.securecall.ui.view.messaging.ChatScreen
import com.example.securecall.ui.viewmodel.CallViewModel
import java.util.UUID

@Composable
fun NavGraph(navController: NavHostController) {

    NavHost(navController = navController, startDestination = "home") {
        composable(route = "launch") { LaunchScreen(
            onNavigateToFaceRecognition = {
                navController.navigate("faceRecognition/auth")
            },
            onNavigateToSignUp = {
                navController.navigate("register")
            }
        )}
        composable(route = "home") { HomeScreen(
            onNavigateToChat = { chatId ->
                navController.navigate("chat/$chatId") {
                    launchSingleTop = true
                    popUpTo("home") {
                        inclusive = false
                    }
                }
            },
            onNavigateToCalls = { // TODO: navigate to CallsScreen
                },
            onNavigateToProfile = { // TODO: navigate to ProfileScreen
                },
            onNavigateToIncomingCall = { callId, callerId, callType ->
                navController.navigate("call/$callId/$callerId/${callType.name}/false")
            },
            onNavigateToNewCall = { // TODO: navigate to NewVideoCallScreen
            },
            onNavigateToSearch = { // TODO: navigate to SearchScreen
            } ,
            onOpenMenu = { // TODO
                }
            )}
        composable(route = "login") { LoginScreen(
            onNavigateToRegister = {
                navController.navigate("register")
            },
            onNavigateToHome = {
                navController.navigate("home")
            },
            onNavigateToForgotPassword = {

            }
        )}
        composable("register") {
            SignUpScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToFaceRegistration = {
                    navController.navigate("faceRecognition/registry")
                }
            )
        }
        composable("faceRecognition/{mode}") { backStackEntry ->
            if (backStackEntry.arguments?.getString("mode") == "registry") {
                RegisterFaceScreen(
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToHome = { navController.navigate("home") }
                )
            } else {
                FaceRecognitionScreen(
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToHome = { navController.navigate("home") },
                    mode = FaceMode.AUTH
                )
            }
        }

        composable("chat/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
            if (chatId == null) {
                // fallback seguro
                return@composable
            }

            ChatScreen(chatId = chatId,
                onBack = { navController.popBackStack() },
                onCall = { receiverId, callType ->
                    val callId = UUID.randomUUID().toString()
                    navController.navigate("call/$callId/$receiverId/${callType.name}/true")
                },
                onMore = { /*TODO*/ }
            )
        }

        composable(
            route = "call/{callId}/{receiverId}/{callType}/{isCaller}",
            arguments = listOf(
                navArgument("callId") {
                    type = NavType.StringType
                },
                navArgument("callType") {
                    type = NavType.StringType
                },
                navArgument("isCaller") {
                    type = NavType.BoolType
                }
            )
        ) { backStackEntry ->

            val callId =
                backStackEntry.arguments?.getString("callId") ?: return@composable

            val receiverId =
                backStackEntry.arguments?.getString("receiverId") ?: return@composable

            val isCaller =
                backStackEntry.arguments?.getBoolean("isCaller") ?: false

            val callType = backStackEntry.arguments
                ?.getString("callType")
                ?.let { runCatching { CallType.valueOf(it) }.getOrNull() }
                ?: CallType.VIDEO

            val viewModel: CallViewModel = hiltViewModel()

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CallScreen(
                callId = callId,
                receiverId = receiverId,
                isCaller = isCaller,
                callType = callType,
                contactName = receiverId,
                uiState = uiState,
                viewModel = viewModel,

                onAccept = {
                    // future incoming flow
                },

                onReject = {
                    viewModel.rejectCall(callId)
                    navController.popBackStack()
                },

                onEndCall = {
                    viewModel.endCall()
                    navController.popBackStack()
                },

                onToggleMic = viewModel::toggleMic,

                onToggleCamera = viewModel::toggleCamera,

                onSwitchCamera = viewModel::switchCamera,

                onToggleSpeaker = viewModel::toggleSpeaker
            )
        }
    }
}
