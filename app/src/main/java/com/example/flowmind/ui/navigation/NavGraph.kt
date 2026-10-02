package com.example.flowmind.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.flowmind.ui.screens.DashboardScreen
import com.example.flowmind.ui.screens.LoginScreen
import com.example.flowmind.ui.screens.WorkflowBuilderScreen

@Composable
fun FlowMindNavGraph(navController: NavHostController, startDestination: String = "login") {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(onLoginSuccess = {
                navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("dashboard") {
            DashboardScreen(
                onNavigateToBuilder = { navController.navigate("builder") }
            )
        }
        composable("builder") {
            WorkflowBuilderScreen()
        }
    }
}
