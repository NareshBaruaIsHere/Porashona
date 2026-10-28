package com.example.ui.theme

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

/**
 * CompositionLocal tracking whether Haptic Feedback is enabled in user settings.
 */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/**
 * Custom [HapticFeedback] implementation that respects the user's Settings toggle
 * and delivers crisp tactile feedback for both Click and LongPress interactions.
 */
class AppHapticFeedback(
    private val view: View,
    private val delegate: HapticFeedback,
    private val enabled: Boolean
) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (!enabled) return
        try {
            when (hapticFeedbackType) {
                HapticFeedbackType.LongPress -> {
                    if (!view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)) {
                        delegate.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                else -> {
                    if (!view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)) {
                        delegate.performHapticFeedback(hapticFeedbackType)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore on unsupported devices or unit test environments
        }
    }
}

/**
 * Performs a subtle tactile click haptic feedback (respecting Settings toggle).
 */
fun HapticFeedback.performClick() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

/**
 * Performs a stronger LongPress tactile haptic feedback (respecting Settings toggle).
 */
fun HapticFeedback.performLongPress() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

/**
 * Clickable modifier that automatically triggers HapticFeedback (Click or LongPress)
 * when the element is tapped or long-pressed, enhancing the Apple-style glassmorphism UI.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.hapticClickable(
    enabled: Boolean = true,
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val haptic = LocalHapticFeedback.current
    return if (onLongClick != null) {
        this.combinedClickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            },
            onClick = {
                haptic.performHapticFeedback(hapticType)
                onClick()
            }
        )
    } else {
        this.clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = {
                haptic.performHapticFeedback(hapticType)
                onClick()
            }
        )
    }
}

@Composable
fun rememberHapticAction(
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    action: () -> Unit
): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic, hapticType, action) {
        {
            haptic.performHapticFeedback(hapticType)
            action()
        }
    }
}
