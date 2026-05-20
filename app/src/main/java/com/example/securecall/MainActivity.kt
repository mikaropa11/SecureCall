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
import com.example.securecall.navigation.NavGraph
import com.example.securecall.ui.theme.SecureCallTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Main Activity
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
}



@Preview(showBackground = true)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    NavGraph(navController = navController)
}