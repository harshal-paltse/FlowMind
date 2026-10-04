package com.example.flowmind.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BlueColorScheme = lightColorScheme(
    primary            = RoyalBlue,
    onPrimary          = PureWhite,
    primaryContainer   = Color(0xFFDDE6FF),
    onPrimaryContainer = RoyalBlue,
    secondary          = Color(0xFF7C3AED),
    onSecondary        = PureWhite,
    secondaryContainer = Color(0xFFEDE9FF),
    onSecondaryContainer = Color(0xFF4C1D95),
    background         = PureWhite,
    onBackground       = InkBlack,
    surface            = LightGray,
    onSurface          = InkBlack,
    error              = Color(0xFFDC2626),
    onError            = PureWhite,
    errorContainer     = Color(0xFFFFE4E4),
    onErrorContainer   = Color(0xFF7F1D1D)
)

@Composable
fun FlowMindTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = BlueColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
