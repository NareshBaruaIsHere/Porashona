package com.example.ui.pomodoro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.appleGlassCard
import com.example.ui.theme.hapticClickable
import com.example.ui.theme.performClick
import com.example.ui.theme.performLongPress
import kotlinx.coroutines.delay

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Dialog shown when pressing Start in Focus mode, asking whether to enable
 * Focus Lockdown (App Pinning + OLED Zen Sanctuary) or run a Standard Timer.
 */
@Composable
fun StartLockdownChoiceDialog(
    subjectName: String,
    plannedMinutes: Int,
    currentStreak: Int,
    onStartLockdown: () -> Unit,
    onStartStandard: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val amberGold = Color(0xFFF59E0B)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = amberGold.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, amberGold.copy(alpha = 0.4f)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = amberGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Focus Mode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = buildString {
                            append("$subjectName • ${plannedMinutes}m")
                            if (currentStreak > 0) append(" • $currentStreak streak")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary Option: Focus Lockdown
                Button(
                    onClick = {
                        haptic.performLongPress()
                        onStartLockdown()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = amberGold,
                        contentColor = Color(0xFF09090B)
                    ),
                    contentPadding = PaddingValues(vertical = 13.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_lockdown_sanctuary_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Focus Lockdown",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                // Secondary Option: Standard Timer
                OutlinedButton(
                    onClick = {
                        haptic.performClick()
                        onStartStandard()
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_standard_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Standard Timer",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    haptic.performClick()
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Full-screen OLED Zen Sanctuary Overlay that activates Android App Pinning (startLockTask),
 * hides system bars in immersive mode, keeps the screen awake, blocks navigation, and enforces
 * a strict 3-second hold-to-break early unlock with a Focus Integrity Streak penalty.
 */
@Composable
fun OledZenSanctuaryOverlay(
    uiState: PomodoroUiState,
    onTogglePauseResume: () -> Unit,
    onBreakLockdownEarly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val amberGold = Color(0xFFF59E0B)
    val crimsonAlert = Color(0xFFEF4444)

    var showBackInterceptBanner by remember { mutableStateOf(false) }
    var showEarlyUnlockPenaltySheet by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHoldingBreak by remember { mutableStateOf(false) }

    // Activate Android App Pinning (startLockTask) + Immersive System Bars + Keep Screen On
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window
        val insetsController = if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else null

        try {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
            activity?.startLockTask()
        } catch (_: Exception) {
            // Safe fallback if device/emulator does not support startLockTask
        }

        onDispose {
            try {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
                activity?.stopLockTask()
            } catch (_: Exception) {
                // Safe fallback if task was not pinned
            }
        }
    }

    // Auto-hide the Back button intercept reminder after 2.5 seconds
    LaunchedEffect(showBackInterceptBanner) {
        if (showBackInterceptBanner) {
            delay(2500)
            showBackInterceptBanner = false
        }
    }

    // 3-second press-and-hold timer for breaking Lockdown early
    LaunchedEffect(isHoldingBreak) {
        if (isHoldingBreak) {
            val steps = 30
            for (step in 1..steps) {
                delay(100)
                holdProgress = step / steps.toFloat()
                if (step % 10 == 0) {
                    haptic.performLongPress()
                }
            }
            isHoldingBreak = false
            holdProgress = 0f
            showEarlyUnlockPenaltySheet = false
            onBreakLockdownEarly()
        } else {
            holdProgress = 0f
        }
    }

    // Intercept hardware Back presses inside the Sanctuary
    BackHandler(enabled = true) {
        haptic.performLongPress()
        if (showEarlyUnlockPenaltySheet) {
            showEarlyUnlockPenaltySheet = false
        } else {
            showBackInterceptBanner = true
        }
    }

    // Breathing ambient OLED radial glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "sanctuary_breathing")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_alpha"
    )
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(durationMillis = 300),
        label = "sanctuary_arc"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030305))
            .testTag("oled_zen_sanctuary_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Breathing Radial Glow on Pure OLED Black
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.52f * breathingScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        amberGold.copy(alpha = breathingAlpha),
                        amberGold.copy(alpha = breathingAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Row: Lockdown Active Pill + Focus Integrity Streak Shield
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Pinned Status Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF14151A),
                        border = BorderStroke(1.dp, amberGold.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = amberGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "LOCKDOWN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = amberGold
                            )
                        }
                    }

                    // Focus Integrity Streak Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF14151A),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = amberGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${uiState.focusIntegrityStreak} Streak",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF4F4F6)
                            )
                        }
                    }
                }

                // Back-intercept gentle reminder banner
                AnimatedVisibility(
                    visible = showBackInterceptBanner,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = crimsonAlert.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, crimsonAlert.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = crimsonAlert,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Locked until timer ends",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Centerpiece: Subject Badge + Glowing OLED Countdown Ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                ) {
                    Text(
                        text = uiState.selectedSubjectName.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp
                        ),
                        color = Color(0xFFE4E4E7),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(276.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 12.dp.toPx()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            style = Stroke(width = strokeWidth)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFD97706),
                                    amberGold,
                                    Color(0xFFFBBF24),
                                    amberGold
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.formattedTime,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 60.sp,
                                letterSpacing = (-1.5).sp
                            ),
                            color = Color.White
                        )

                        if (uiState.status == PomodoroStatus.PAUSED) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.sp
                                ),
                                color = amberGold.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Bottom Controls: Pause/Resume & Strict Emergency Unlock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showEarlyUnlockPenaltySheet) {
                    // Strict Early Unlock Penalty Card with 3-Second Press-and-Hold
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF14151A),
                        border = BorderStroke(1.dp, crimsonAlert.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("early_unlock_penalty_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Exit Lockdown?",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )

                            // 3-Second Hold-to-Break Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(crimsonAlert.copy(alpha = 0.16f))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                haptic.performLongPress()
                                                isHoldingBreak = true
                                                tryAwaitRelease()
                                                isHoldingBreak = false
                                            }
                                        )
                                    }
                                    .testTag("hold_to_break_lockdown_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(holdProgress)
                                        .align(Alignment.CenterStart)
                                        .background(crimsonAlert.copy(alpha = 0.65f))
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isHoldingBreak) {
                                            "Holding… ${(3f - holdProgress * 3f).coerceAtLeast(0.1f).let { String.format(java.util.Locale.US, "%.1fs", it) }}"
                                        } else {
                                            "Hold 3s to Unlock"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        haptic.performLongPress()
                                        showEarlyUnlockPenaltySheet = false
                                        onBreakLockdownEarly()
                                    },
                                    modifier = Modifier.testTag("instant_break_lockdown_button")
                                ) {
                                    Text(
                                        text = "Unlock Now",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = crimsonAlert.copy(alpha = 0.85f)
                                    )
                                }

                                Button(
                                    onClick = {
                                        haptic.performClick()
                                        showEarlyUnlockPenaltySheet = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = amberGold,
                                        contentColor = Color(0xFF09090B)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Stay",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause / Resume FAB
                        FloatingActionButton(
                            onClick = {
                                haptic.performLongPress()
                                onTogglePauseResume()
                            },
                            containerColor = amberGold,
                            contentColor = Color(0xFF09090B),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("sanctuary_pause_resume_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.status == PomodoroStatus.RUNNING) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = if (uiState.status == PomodoroStatus.RUNNING) "Pause" else "Resume",
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Emergency Unlock Trigger Pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .hapticClickable {
                                    showEarlyUnlockPenaltySheet = true
                                }
                                .testTag("emergency_unlock_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Unlock",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFFD4D4D8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog shown after the user breaks a Focus Lockdown early, confirming the streak penalty.
 */
@Composable
fun StreakBrokenAlertDialog(
    lostStreakCount: Int,
    onDismiss: () -> Unit
) {
    val crimsonAlert = Color(0xFFEF4444)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GppBad,
                    contentDescription = null,
                    tint = crimsonAlert,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Streak Reset",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Text(
                text = if (lostStreakCount > 0) {
                    "Exited early. $lostStreakCount-session streak reset to 0."
                } else {
                    "Exited early. Complete a full session to build your streak."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_streak_broken_button")
            ) {
                Text("OK")
            }
        }
    )
}
