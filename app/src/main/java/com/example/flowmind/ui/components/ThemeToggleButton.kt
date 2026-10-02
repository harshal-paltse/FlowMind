package com.example.flowmind.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.flowmind.ui.theme.ThemeViewModel

@Composable
fun ThemeToggleButton(themeViewModel: ThemeViewModel) {
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()

    IconButton(onClick = { themeViewModel.toggleTheme() }) {
        Icon(
            imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = "Toggle Theme",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
