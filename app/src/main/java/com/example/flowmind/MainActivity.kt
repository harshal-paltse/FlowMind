package com.example.flowmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.flowmind.ui.screens.LoginScreen
import com.example.flowmind.ui.theme.FlowMindTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowMindTheme {
                val navController = androidx.navigation.compose.rememberNavController()
                com.example.flowmind.ui.navigation.FlowMindNavGraph(navController = navController)
            }
        }
    }
}
