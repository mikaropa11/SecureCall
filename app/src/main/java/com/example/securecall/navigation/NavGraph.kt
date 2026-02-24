package com.example.securecall.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.securecall.ui.view.HomeScreen
import com.example.securecall.ui.view.auth.LaunchScreen
import com.example.securecall.ui.view.auth.LoginScreen
import com.example.securecall.ui.view.auth.SignUpScreen

@Composable
fun NavGraph(navController: NavHostController) {

    NavHost(navController = navController, startDestination = "launch") {
        composable(route = "launch") { LaunchScreen(
            onNavigateToLogin = {
                navController.navigate("login")
            },
            onNavigateToSignUp = {
                navController.navigate("register")
            }
        )}
        composable(route = "home") { HomeScreen(navController) }
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
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }
    }
}