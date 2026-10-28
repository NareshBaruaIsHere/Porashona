package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Titanium Obsidian & Warm Amber Gold Palette (Dark Mode)
val DarkBackground = Color(0xFF090A0F)
val DarkSurface = Color(0xFF131621)
val DarkSurfaceVariant = Color(0xFF22283B)
val DarkBorder = Color(0xFF434E70)
val PrimaryDark = Color(0xFFFBBF24)          // Warm Amber Gold
val OnPrimaryDark = Color(0xFF241600)
val PrimaryContainerDark = Color(0xFF3B2A07)
val OnPrimaryContainerDark = Color(0xFFFEF3C7)
val SecondaryDark = Color(0xFF38BDF8)        // Sky Ice Accent
val SecondaryContainerDark = Color(0xFF0F293D)
val OnSecondaryContainerDark = Color(0xFFE0F2FE)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)

// Warm Stone & Brushed Bronze-Gold Palette (High-Contrast Light Mode)
val LightBackground = Color(0xFFEDE8DD)      // Warm Limestone canvas so white cards pop clearly
val LightSurface = Color(0xFFFFFFFF)         // Crisp Pure Ivory-White elevated card surface
val LightSurfaceVariant = Color(0xFFE1D9C8)  // Distinct Warm Stone for inner boxes & pills
val LightBorder = Color(0xFFB5AA94)          // Crisp, clearly visible structural border
val PrimaryLight = Color(0xFFB45309)         // High-contrast Rich Amber Bronze
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFFDE68A)
val OnPrimaryContainerLight = Color(0xFF451A03)
val SecondaryLight = Color(0xFF0369A1)
val SecondaryContainerLight = Color(0xFFE0F2FE)
val OnSecondaryContainerLight = Color(0xFF075985)
val TextPrimaryLight = Color(0xFF0F172A)     // Deep Obsidian Slate
val TextSecondaryLight = Color(0xFF334155)   // High-contrast Slate for secondary text

// Semantic Status Accents
val SuccessGreen = Color(0xFF10B981)
val WarningOrange = Color(0xFFF59E0B)
val DangerRed = Color(0xFFF43F5E)

// Soft Subject Accent Palette
val SubjectBlue = Color(0xFF38BDF8)
val SubjectEmerald = Color(0xFF34D399)
val SubjectPurple = Color(0xFFA78BFA)
val SubjectAmber = Color(0xFFFBBF24)
val SubjectRose = Color(0xFFFB7185)
val SubjectCyan = Color(0xFF2DD4BF)

val PresetColorHexes = listOf(
    "#FBBF24", // Warm Amber Gold
    "#38BDF8", // Sky Ice
    "#FB7185", // Coral Rose
    "#A78BFA", // Soft Violet
    "#34D399", // Emerald
    "#F97316", // Sunset Orange
    "#60A5FA", // Ocean Blue
    "#F472B6"  // Rose Pink
)

/**
 * Ensures bright pastel/gold subject accents remain high-contrast and readable
 * when rendered as text, numbers, or icons on light surfaces.
 */
fun Color.readableOnSurface(isDark: Boolean): Color {
    if (isDark) return this
    return Color(
        red = (red * 0.72f).coerceIn(0f, 1f),
        green = (green * 0.66f).coerceIn(0f, 1f),
        blue = (blue * 0.72f).coerceIn(0f, 1f),
        alpha = alpha
    )
}
