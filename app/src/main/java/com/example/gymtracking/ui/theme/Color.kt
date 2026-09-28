package com.example.gymtracking.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// STEALTH MATTE — Low-Key, Distraction-Free Dark Palette
// ═══════════════════════════════════════════════════════════════

// Backgrounds
val MidnightBlack = Color(0xFF121212)
val MidnightSurface = Color(0xFF1E1E1E)
val MidnightElevated = Color(0xFF2C2C2C)
val MidnightBorder = Color(0xFF3A3A3A)

// Primary — Muted Slate
val ElectricViolet = Color(0xFF546E7A)
val ElectricVioletDim = Color(0xFF37474F)
val ElectricVioletContainer = Color(0xFF263238)
val OnElectricVioletContainer = Color(0xFFCFD8DC)

// Secondary — Muted Teal
val NeonCyan = Color(0xFF4DB6AC)
val NeonCyanDim = Color(0xFF00897B)
val NeonCyanContainer = Color(0xFF004D40)
val OnNeonCyanContainer = Color(0xFFB2DFDB)

// Tertiary — Muted Rust
val HotCoral = Color(0xFF8D6E63)
val HotCoralContainer = Color(0xFF4E342E)

// Text
val TextPrimary = Color(0xFFE0E0E0)
val TextSecondary = Color(0xFF9E9E9E)
val TextMuted = Color(0xFF757575)

// Semantic (Desaturated)
val TrophyGold = Color(0xFFBCAAA4)
val TrophyGoldBgDark = Color(0xFF3E2723)
val TrophyGoldBgLight = Color(0xFFEFEBE9)
val SuccessGreen = Color(0xFF81C784)
val WarningAmber = Color(0xFFFFD54F)
val ErrorRose = Color(0xFFE57373)

// Light theme variants (for fallback)
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFAFAFA)
val LightSurfaceElevated = Color(0xFFEEEEEE)
val LightBorder = Color(0xFFE0E0E0)

fun getCategoryColor(category: String): Color {
    // Desaturated category colors for a stealthy look
    return when (category.trim().lowercase()) {
        "chest" -> Color(0xFFE57373)
        "back" -> Color(0xFF64B5F6)
        "legs" -> Color(0xFF81C784)
        "arms", "biceps", "triceps" -> Color(0xFFFFD54F)
        "shoulders" -> Color(0xFFBA68C8)
        "core", "abs" -> Color(0xFFF06292)
        "cardio" -> Color(0xFF4DD0E1)
        else -> Color(0xFF90A4AE)
    }
}