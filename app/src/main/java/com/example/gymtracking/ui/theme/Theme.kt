package com.example.gymtracking.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MidnightNeonDark = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = ElectricVioletContainer,
    onPrimaryContainer = OnElectricVioletContainer,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = NeonCyanContainer,
    onSecondaryContainer = OnNeonCyanContainer,
    tertiary = HotCoral,
    background = MidnightBlack,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightElevated,
    onSurfaceVariant = TextSecondary,
    outlineVariant = MidnightBorder,
    error = ErrorRose,
    errorContainer = HotCoralContainer
)

private val MidnightNeonLight = lightColorScheme(
    primary = ElectricVioletDim,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = NeonCyanDim,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = HotCoral,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = Color(0xFF475569),
    outlineVariant = LightBorder,
    error = ErrorRose
)

@Composable
fun GymTrackingTheme(
    darkTheme: Boolean = true, // Force dark by default for premium look
    dynamicColor: Boolean = false, // Disabled — we own the palette
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MidnightNeonDark else MidnightNeonLight

    // Tint the system bars to match
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}