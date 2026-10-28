package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lightweight Apple-style Glassmorphism Card Modifier.
 * Memoizes gradient brushes across recompositions for 60/120fps scroll performance
 * with strong Light/Dark contrast.
 */
@Composable
fun Modifier.appleGlassCard(
    shape: Shape = RoundedCornerShape(22.dp),
    accentTint: Color? = null,
    borderWidth: Dp = 1.dp
): Modifier {
    val bgColor = MaterialTheme.colorScheme.background
    val baseSurface = MaterialTheme.colorScheme.surface
    val isDark = remember(bgColor) { bgColor.luminance() < 0.5f }

    val (surfaceBrush, borderBrush) = remember(isDark, accentTint, baseSurface) {
        val topSurfaceColor = if (isDark) {
            if (accentTint != null) {
                Color(
                    red = (baseSurface.red * 0.86f + accentTint.red * 0.14f),
                    green = (baseSurface.green * 0.86f + accentTint.green * 0.14f),
                    blue = (baseSurface.blue * 0.86f + accentTint.blue * 0.14f),
                    alpha = 0.88f
                )
            } else {
                Color(0xFF1A1D2B).copy(alpha = 0.86f)
            }
        } else {
            if (accentTint != null) {
                Color(
                    red = (0.99f * 0.92f + accentTint.red * 0.08f),
                    green = (0.99f * 0.92f + accentTint.green * 0.08f),
                    blue = (0.99f * 0.92f + accentTint.blue * 0.08f),
                    alpha = 0.98f
                )
            } else {
                Color(0xFFFFFFFF)
            }
        }

        val bottomSurfaceColor = if (isDark) {
            Color(0xFF11131C).copy(alpha = 0.80f)
        } else {
            Color(0xFFFBF9F4)
        }

        val rimHighlightStart = if (isDark) {
            (accentTint ?: Color.White).copy(alpha = if (accentTint != null) 0.48f else 0.28f)
        } else {
            if (accentTint != null) {
                accentTint.readableOnSurface(isDark = false).copy(alpha = 0.45f)
            } else {
                Color(0xFFB5AA94)
            }
        }

        val rimHighlightEnd = if (isDark) {
            Color(0xFF434E70).copy(alpha = 0.65f)
        } else {
            Color(0xFFC4B9A3)
        }

        Brush.verticalGradient(listOf(topSurfaceColor, bottomSurfaceColor)) to
            Brush.linearGradient(
                colors = listOf(rimHighlightStart, rimHighlightEnd),
                start = Offset(0f, 0f),
                end = Offset(600f, 600f)
            )
    }

    return this
        .clip(shape)
        .background(brush = surfaceBrush, shape = shape)
        .border(width = borderWidth, brush = borderBrush, shape = shape)
}

/**
 * Floating Apple-style Translucent Glass Dock Modifier for the bottom navigation bar.
 */
@Composable
fun Modifier.appleGlassDock(
    shape: Shape = RoundedCornerShape(28.dp)
): Modifier {
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = remember(bgColor) { bgColor.luminance() < 0.5f }

    val (surfaceBrush, borderBrush) = remember(isDark) {
        val topColor = if (isDark) Color(0xFF1E2233).copy(alpha = 0.92f) else Color(0xFFFFFFFF).copy(alpha = 0.96f)
        val bottomColor = if (isDark) Color(0xFF12141F).copy(alpha = 0.88f) else Color(0xFFF5F0E6).copy(alpha = 0.94f)
        val topRim = if (isDark) Color(0xFFFBBF24).copy(alpha = 0.35f) else Color(0xFFB45309).copy(alpha = 0.38f)
        val bottomRim = if (isDark) Color(0xFF434E70).copy(alpha = 0.70f) else Color(0xFFB5AA94)

        Brush.verticalGradient(listOf(topColor, bottomColor)) to
            Brush.verticalGradient(listOf(topRim, bottomRim))
    }

    return this
        .clip(shape)
        .background(brush = surfaceBrush, shape = shape)
        .border(width = 1.dp, brush = borderBrush, shape = shape)
}

/**
 * Lightweight Apple-style Glass Pill Modifier for header/status capsules.
 */
@Composable
fun Modifier.appleGlassPill(
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier {
    val bgColor = MaterialTheme.colorScheme.background
    val isDark = remember(bgColor) { bgColor.luminance() < 0.5f }

    val (surfaceBrush, rimColor) = remember(isDark) {
        val topColor = if (isDark) Color(0xFF202538).copy(alpha = 0.92f) else Color(0xFFFFFFFF)
        val bottomColor = if (isDark) Color(0xFF151926).copy(alpha = 0.88f) else Color(0xFFF6F2E9)
        val rim = if (isDark) Color(0xFF4A567A) else Color(0xFFBEB39D)
        Brush.verticalGradient(listOf(topColor, bottomColor)) to rim
    }

    return this
        .clip(shape)
        .background(brush = surfaceBrush, shape = shape)
        .border(width = 1.dp, color = rimColor, shape = shape)
}
