package com.example.securecall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.securecall.domain.model.UserStatus
import com.example.securecall.domain.repository.UserRepository
import com.example.securecall.navigation.NavGraph
import com.example.securecall.ui.theme.SecureCallTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The Main Activity
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var isChecking = true
        lifecycleScope.launch {
            delay(1500L)
            isChecking = false
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                isChecking
            }
        }

        setContent {
            SecureCallTheme {
                MainScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updatePresence(UserStatus.online)
    }

    override fun onStop() {
        updatePresence(UserStatus.offline)
        super.onStop()
    }

    private fun updatePresence(status: UserStatus) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        lifecycleScope.launch {
            userRepository.updateUserStatus(userId, status)
        }
    }
}



@Preview(showBackground = true)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    NavGraph(navController = navController)
}
