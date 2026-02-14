package com.example.securecall.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.securecall.ui.view.CallRegistryScreen
import com.example.securecall.ui.view.HomeScreen
import com.example.securecall.ui.view.SignUpScreen

@Composable
fun NavGraph(navController: NavHostController) {

    NavHost(navController = navController, startDestination = "home") {
        composable(route = "home") { HomeScreen(navController) }
    }
}