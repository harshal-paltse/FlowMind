package com.example.flowmind.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.flowmind.ui.screens.*

@Composable
fun FlowMindNavGraph(navController: NavHostController, startDestination: String = "login") {
    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(
                onLoginSuccess    = { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } },
                onRegisterClicked = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegistrationScreen(onRegistrationComplete = {
                navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
            })
        }

        composable("dashboard") {
            DashboardScreen(
                onNavigateToBuilder   = { navController.navigate("builder") },
                onNavigateToModels    = { navController.navigate("models") },
                onNavigateToLiveInput = { navController.navigate("live_input") },
                onNavigateToReports   = { navController.navigate("reports") },
                onNavigateToLab       = { navController.navigate("lab") },
                onNavigateToSchedule  = { navController.navigate("schedule") },
                onRunWorkflow         = { wfId -> navController.navigate("result/$wfId") }
            )
        }

        composable("builder") { WorkflowBuilderScreen() }

        composable("models") {
            ModelCandidatesScreen(onBackClicked = { navController.popBackStack() })
        }

        composable("live_input") {
            LiveInputScreen(onBackClicked = { navController.popBackStack() })
        }

        composable("reports") {
            ReportsScreen(onBackClicked = { navController.popBackStack() })
        }

        composable("lab") {
            WorkflowLabScreen(onBackClicked = { navController.popBackStack() })
        }

        composable("schedule") {
            ScheduleScreen(onBackClicked = { navController.popBackStack() })
        }

        composable("diagnostics") {
            DiagnosticsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "result/{workflowId}",
            arguments = listOf(navArgument("workflowId") { type = NavType.StringType })
        ) { backStack ->
            val wfId = backStack.arguments?.getString("workflowId") ?: return@composable
            val dashVM: DashboardViewModel = hiltViewModel()
            val workflows by dashVM.workflows.collectAsState()
            val wf = workflows.firstOrNull { it.id == wfId }
            if (wf != null) {
                ResultScreen(workflow = wf, onBack = { navController.popBackStack() })
            }
        }
    }
}
