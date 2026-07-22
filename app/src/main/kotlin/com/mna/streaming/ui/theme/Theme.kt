package com.mna.streaming.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

val MADark = Color(0xFF0A0A0F)
val MASurface = Color(0xFF141414)
val MACard = Color(0xFF1E1E24)
val MATextPrimary = Color(0xFFFFFFFF)
val MATextSecondary = Color(0xFFB3B3B3)
val MAGold = Color(0xFFFFD700)

private val DarkColorScheme = darkColorScheme(
    primary = MARed,
    onPrimary = Color.White,
    secondary = MAGold,
    onSecondary = Color.Black,
    background = MADark,
    onBackground = MATextPrimary,
    surface = MASurface,
    onSurface = MATextPrimary,
    surfaceVariant = MACard,
    onSurfaceVariant = MATextSecondary,
    error = Color(0xFFCF6679),
)

@Composable
fun MATheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make the system navigation bar match the app's near-black background
            // so the 3-button row blends seamlessly instead of using the system default.
            @Suppress("DEPRECATION")
            window.navigationBarColor = MADark.toArgb()
            WindowInsetsControllerCompat(window, view)
                .isAppearanceLightNavigationBars = false  // keep icons white/light
        }
    }
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MATypography,
        content = content
    )
}
