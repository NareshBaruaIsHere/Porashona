package com.example.ui.pomodoro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.ExamUrgencyStatus
import com.example.ui.theme.appleGlassCard
import com.example.ui.theme.appleGlassPill
import com.example.ui.theme.hapticClickable
import com.example.ui.theme.performClick
import com.example.ui.theme.performLongPress
import com.example.ui.theme.readableOnSurface
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    LaunchedEffect(uiState.completionFeedbackPulse) {
        if (uiState.completionFeedbackPulse > 0) {
            haptic.performLongPress()
        }
    }

    var showSubjectMenu by remember { mutableStateOf(false) }
    var showCustomMinutesDialog by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf("30") }
    var showDailyTargetDialog by remember { mutableStateOf(false) }
    var dailyTargetInput by remember { mutableStateOf("120") }

    val isFocusMode = uiState.mode == PomodoroMode.FOCUS
    val accentColor = if (isFocusMode) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF0284C7).readableOnSurface(isDark)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Sleek Daily Focus Target Card (Apple Glassmorphic + Smooth Capsule Progress)
            item(key = "daily_focus_target_card") {
                DailyFocusTargetCard(
                    completedMinutesToday = uiState.completedMinutesToday,
                    dailyTargetMinutes = uiState.dailyTargetMinutes,
                    remainingMinutes = uiState.remainingDailyMinutes,
                    progress = uiState.dailyTargetProgress,
                    completedSessionsToday = uiState.completedSessionsToday,
                    plannedSessionMinutes = uiState.plannedSessionMinutes,
                    focusIntegrityStreak = uiState.focusIntegrityStreak,
                    isRunning = uiState.status == PomodoroStatus.RUNNING,
                    onEditTarget = {
                        dailyTargetInput = uiState.dailyTargetMinutes.toString()
                        showDailyTargetDialog = true
                    }
                )
            }

            // 2. Unified 2-Option Mode Switcher: Focus & Break (No redundant Short/Long Break split)
            item(key = "mode_tabs") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleGlassPill(shape = RoundedCornerShape(18.dp))
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val modeOptions = listOf(
                            PomodoroMode.FOCUS to stringResource(R.string.mode_focus),
                            PomodoroMode.SHORT_BREAK to "Break"
                        )
                        modeOptions.forEach { (targetMode, label) ->
                            val isSelected = if (targetMode == PomodoroMode.FOCUS) {
                                isFocusMode
                            } else {
                                !isFocusMode
                            }
                            val tabAccent = if (targetMode == PomodoroMode.FOCUS) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color(0xFF0284C7).readableOnSurface(isDark)
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) tabAccent.copy(alpha = if (isDark) 0.22f else 0.14f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, tabAccent.copy(alpha = 0.50f)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .hapticClickable { viewModel.setMode(targetMode) }
                                    .testTag("pomodoro_mode_${targetMode.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) tabAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Centered Apple-Glass Focus Subject Selector Pill (Opens Centered Glass Modal Dialog)
            item(key = "subject_selector") {
                val rawSubjectColor = remember(uiState.selectedSubjectColor) {
                    try {
                        Color(android.graphics.Color.parseColor(uiState.selectedSubjectColor))
                    } catch (e: Exception) {
                        Color(0xFF0D9488)
                    }
                }
                val subjectColor = rawSubjectColor.readableOnSurface(isDark)
                val chevronRotation by animateFloatAsState(
                    targetValue = if (showSubjectMenu) 180f else 0f,
                    animationSpec = tween(durationMillis = 220),
                    label = "subject_chevron_rotation"
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Centered Trigger Pill
                    Box(
                        modifier = Modifier
                            .appleGlassPill(shape = RoundedCornerShape(50))
                            .hapticClickable { showSubjectMenu = true }
                            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
                            .testTag("pomodoro_subject_selector")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Tinted Squircle Subject Dot Badge
                            Surface(
                                shape = CircleShape,
                                color = subjectColor.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.45f)),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(subjectColor)
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = "FOCUS SUBJECT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.7.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.selectedSubjectName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            uiState.selectedSubjectTarget?.let { target ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = subjectColor.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "${target.readiness.score}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = subjectColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select Focus Subject",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .rotate(chevronRotation)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Big Circular Countdown Timer (High-Contrast Track & Ring Outline in both Light & Dark Mode)
            item(key = "timer_circle") {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = uiState.progress,
                        animationSpec = tween(durationMillis = 300),
                        label = "pomo_arc"
                    )

                    val trackColor = if (isDark) {
                        Color(0xFF283048)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f)
                    }
                    val trackRimColor = if (isDark) {
                        Color(0xFF4A567A)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val borderStrokeWidth = 1.2.dp.toPx()
                        val baseRadius = (size.minDimension - strokeWidth) / 2f

                        // Main circular track fill
                        drawCircle(
                            color = trackColor,
                            radius = baseRadius,
                            style = Stroke(width = strokeWidth)
                        )
                        // Crisp outer & inner track borders so the timer ring is clearly visible in Dark Mode
                        drawCircle(
                            color = trackRimColor,
                            radius = baseRadius + (strokeWidth / 2f),
                            style = Stroke(width = borderStrokeWidth)
                        )
                        drawCircle(
                            color = trackRimColor,
                            radius = baseRadius - (strokeWidth / 2f),
                            style = Stroke(width = borderStrokeWidth)
                        )
                        // Active progress arc
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .hapticClickable {
                                customMinutesInput = (uiState.totalSeconds / 60).toString()
                                showCustomMinutesDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag(if (isFocusMode) "pomodoro_custom_time_button" else "pomodoro_timer_display"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.formattedTime,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 54.sp,
                                letterSpacing = (-1).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 5. Duration Presets (Shown only in Break Mode: 5m, 10m, 15m, Custom)
            if (!isFocusMode) {
                item(key = "presets") {
                    val presets = listOf(5 to "5m", 10 to "10m", 15 to "15m")
                    val currentMinutes = uiState.totalSeconds / 60
                    val isCustomSelected = presets.none { it.first == currentMinutes }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presets.forEach { (mins, label) ->
                            val isSelected = currentMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .hapticClickable {
                                        viewModel.setCustomBreakMinutes(mins)
                                    }
                                    .testTag("pomodoro_preset_${mins}m")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                )
                            }
                        }

                        // Custom Break Duration Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCustomSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isCustomSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .hapticClickable {
                                    customMinutesInput = currentMinutes.toString()
                                    showCustomMinutesDialog = true
                                }
                                .testTag("pomodoro_custom_time_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isCustomSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isCustomSelected) {
                                        "${stringResource(R.string.custom_time)} (${currentMinutes}m)"
                                    } else {
                                        stringResource(R.string.custom_time)
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCustomSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 6. Controls Row (Reset, Play/Pause, Stop & Save, Skip) with tactile HapticFeedback
            item(key = "controls") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button
                    IconButton(
                        onClick = {
                            haptic.performClick()
                            viewModel.resetTimer()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pomodoro_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset timer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Big Play/Pause FAB (Asks whether to enable Focus Lockdown when starting Focus from IDLE)
                    FloatingActionButton(
                        onClick = {
                            haptic.performLongPress()
                            viewModel.onPlayButtonClicked()
                        },
                        containerColor = accentColor,
                        contentColor = if (isFocusMode) MaterialTheme.colorScheme.onPrimary else Color.White,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("pomodoro_play_pause_button"),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (uiState.status == PomodoroStatus.RUNNING) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (uiState.status == PomodoroStatus.RUNNING) "Pause" else "Play",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Stop & Save Button
                    IconButton(
                        onClick = {
                            haptic.performLongPress()
                            viewModel.stopAndSaveTimer()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pomodoro_stop_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (uiState.status == PomodoroStatus.RUNNING || uiState.status == PomodoroStatus.PAUSED) {
                                Color(0xFFEF4444).copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = stringResource(R.string.stop_timer),
                                    tint = if (uiState.status == PomodoroStatus.RUNNING || uiState.status == PomodoroStatus.PAUSED) {
                                        Color(0xFFEF4444)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Skip Button
                    IconButton(
                        onClick = {
                            haptic.performClick()
                            viewModel.skip()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pomodoro_skip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip mode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 7. Redesigned Subject Focus Targets (Apple Glassmorphic + Smooth Capsule Progress)
            if (uiState.subjectFocusTargets.isNotEmpty()) {
                item(key = "subject_targets_section") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassCard(shape = RoundedCornerShape(22.dp))
                            .testTag("subject_focus_targets_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Subject Focus Targets",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap to select",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            uiState.subjectFocusTargets.take(5).forEach { target ->
                                val isSelected = uiState.selectedSubjectId == target.subject.id
                                val rawColor = remember(target.subject.colorTag) {
                                    try {
                                        Color(android.graphics.Color.parseColor(target.subject.colorTag))
                                    } catch (e: Exception) {
                                        Color(0xFF0D9488)
                                    }
                                }
                                val subjectColor = rawColor.readableOnSurface(isDark)
                                val urgencyColor = when (target.readiness.urgencyStatus) {
                                    ExamUrgencyStatus.HIGH_URGENCY -> Color(0xFFDC2626)
                                    ExamUrgencyStatus.NEEDS_ATTENTION -> Color(0xFFD97706)
                                    ExamUrgencyStatus.ON_TRACK -> Color(0xFF0284C7)
                                    ExamUrgencyStatus.READY -> Color(0xFF059669)
                                    ExamUrgencyStatus.COMPLETED -> Color(0xFF6B7280)
                                }.readableOnSurface(isDark)

                                val progressFraction = if (target.plannedTargetMinutes > 0) {
                                    (target.totalLoggedMinutes.toFloat() / target.plannedTargetMinutes.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                val initials = remember(target.subject.name) {
                                    target.subject.name
                                        .trim()
                                        .split(Regex("\\s+"))
                                        .filter { it.isNotBlank() }
                                        .take(2)
                                        .joinToString("") { it.first().uppercase() }
                                        .ifBlank { "SB" }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) {
                                        subjectColor.copy(alpha = if (isDark) 0.14f else 0.10f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.65f)
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) subjectColor else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .hapticClickable(
                                            hapticType = HapticFeedbackType.LongPress
                                        ) {
                                            viewModel.selectSubject(target.subject)
                                        }
                                        .testTag("subject_target_item_${target.subject.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                // Squircle Subject Badge
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = subjectColor.copy(alpha = 0.18f),
                                                    border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.35f)),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Selected",
                                                                tint = subjectColor,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        } else {
                                                            Text(
                                                                text = initials,
                                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = subjectColor
                                                            )
                                                        }
                                                    }
                                                }

                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        text = target.subject.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val subMeta = if (target.daysRemaining >= 0) {
                                                        "${target.totalLoggedMinutes}m / ${target.plannedTargetMinutes}m • ${target.daysRemaining}d left"
                                                    } else {
                                                        "${target.totalLoggedMinutes}m / ${target.plannedTargetMinutes}m"
                                                    }
                                                    Text(
                                                        text = subMeta,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = urgencyColor.copy(alpha = 0.14f),
                                                    border = BorderStroke(1.dp, urgencyColor.copy(alpha = 0.30f))
                                                ) {
                                                    Text(
                                                        text = "${target.readiness.score}% Ready",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = urgencyColor,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                if (target.daysRemaining >= 0) {
                                                    Text(
                                                        text = "${target.readiness.recommendedDailyMinutes}m/day",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Smooth Custom Capsule Progress Bar (No M3 stop-dot artifacts)
                                        SmoothCapsuleProgressBar(
                                            progress = progressFraction,
                                            fillColor = subjectColor,
                                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.65f else 0.9f),
                                            height = 6.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. Recent Completed Focus Sessions Log
            if (uiState.recentCompletedSessions.isNotEmpty()) {
                item(key = "recent_sessions_card") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleGlassCard(shape = RoundedCornerShape(22.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Recent Sessions",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            uiState.recentCompletedSessions.take(4).forEach { session ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981).readableOnSurface(isDark),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.subjectName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = DateUtils.formatExamDate(session.dateMillis),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = "+${session.durationMinutes}m",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full-Screen Immersive OLED Zen Sanctuary Lockdown Overlay
        if (uiState.isLockdownActive) {
            OledZenSanctuaryOverlay(
                uiState = uiState,
                onTogglePauseResume = {
                    if (uiState.status == PomodoroStatus.RUNNING) {
                        viewModel.pauseTimer()
                    } else {
                        viewModel.startTimer()
                    }
                },
                onBreakLockdownEarly = {
                    viewModel.breakLockdownEarly()
                }
            )
        }
    }

    // Centered Glass Modal Dialog for Choosing Focus Subject (No Tips, Always Centered)
    if (showSubjectMenu) {
        Dialog(onDismissRequest = { showSubjectMenu = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .appleGlassCard(
                        shape = RoundedCornerShape(24.dp),
                        accentTint = MaterialTheme.colorScheme.primary
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "CHOOSE FOCUS TARGET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Select Subject",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .hapticClickable { showSubjectMenu = false }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Option 1: General Study
                        val isGeneralSelected = uiState.selectedSubjectId == null
                        val generalColor = Color(0xFF0D9488).readableOnSurface(isDark)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isGeneralSelected) {
                                generalColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.45f)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isGeneralSelected) generalColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .hapticClickable {
                                    viewModel.selectSubject(null)
                                    showSubjectMenu = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = generalColor.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, generalColor.copy(alpha = 0.40f)),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(generalColor)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "General Study",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isGeneralSelected) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Open focus session",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (isGeneralSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = generalColor,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Subject Options (from Exams & Routine)
                        uiState.availableSubjects.forEach { sub ->
                            val isSelected = uiState.selectedSubjectId == sub.id
                            val rawSubColor = remember(sub.colorTag) {
                                try {
                                    Color(android.graphics.Color.parseColor(sub.colorTag))
                                } catch (e: Exception) {
                                    Color(0xFF0D9488)
                                }
                            }
                            val subColor = rawSubColor.readableOnSurface(isDark)
                            val targetForSub = uiState.subjectFocusTargets.firstOrNull { it.subject.id == sub.id }
                            val subInitials = remember(sub.name) {
                                sub.name.trim()
                                    .split(Regex("\\s+"))
                                    .filter { it.isNotBlank() }
                                    .take(2)
                                    .joinToString("") { it.first().uppercase() }
                                    .ifBlank { "SB" }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    subColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.45f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) subColor.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .hapticClickable {
                                        viewModel.selectSubject(sub)
                                        showSubjectMenu = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = subColor.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, subColor.copy(alpha = 0.40f)),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = subInitials,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = subColor
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sub.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val subtitle = when {
                                                targetForSub != null && targetForSub.daysRemaining >= 0 ->
                                                    "${targetForSub.readiness.score}% Ready • ${targetForSub.daysRemaining}d left"
                                                targetForSub != null ->
                                                    "${targetForSub.readiness.score}% Ready"
                                                sub.roomNumber.isNotBlank() ->
                                                    "Routine • ${sub.roomNumber}"
                                                else ->
                                                    "Routine Class"
                                            }
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Surface(
                                            shape = CircleShape,
                                            color = subColor,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Start Session Mode Choice Dialog (Ask whether to enable Lockdown when pressing Start)
    if (uiState.showStartLockdownPrompt) {
        StartLockdownChoiceDialog(
            subjectName = uiState.selectedSubjectName,
            plannedMinutes = uiState.plannedSessionMinutes,
            currentStreak = uiState.focusIntegrityStreak,
            onStartLockdown = {
                viewModel.startFocusWithChoice(enableLockdown = true)
            },
            onStartStandard = {
                viewModel.startFocusWithChoice(enableLockdown = false)
            },
            onDismiss = {
                viewModel.dismissStartLockdownPrompt()
            }
        )
    }

    // Streak Broken Penalty Alert Dialog
    if (uiState.streakBrokenAlert) {
        StreakBrokenAlertDialog(
            lostStreakCount = uiState.lostStreakCount,
            onDismiss = { viewModel.dismissStreakBrokenAlert() }
        )
    }

    // Celebration Dialog when focus session finishes
    if (uiState.showCelebrationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCelebrationDialog() },
            title = {
                Text(
                    text = if (uiState.lastSessionWasLockdown) {
                        "Lockdown Sanctuary Complete!"
                    } else {
                        stringResource(R.string.session_finished_title)
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(
                            R.string.session_finished_message,
                            uiState.lastCompletedMinutes,
                            uiState.selectedSubjectName
                        )
                    )
                    if (uiState.lastSessionWasLockdown) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Focus Integrity Streak: ${uiState.focusIntegrityStreak} Unbroken Sessions (+1)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    uiState.selectedSubjectTarget?.let { target ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Updated ${target.subject.name} Readiness: ${target.readiness.score}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performClick()
                        viewModel.dismissCelebrationDialog()
                        viewModel.setMode(PomodoroMode.SHORT_BREAK)
                        viewModel.startTimer()
                    }
                ) {
                    Text(stringResource(R.string.start_break))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.performClick()
                        viewModel.dismissCelebrationDialog()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Custom Minutes Dialog
    if (showCustomMinutesDialog) {
        AlertDialog(
            onDismissRequest = { showCustomMinutesDialog = false },
            title = {
                Text(
                    text = if (isFocusMode) "Custom Focus Time" else "Custom Break Time",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter duration in minutes (1 - 240):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customMinutesInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                customMinutesInput = input
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performClick()
                        val mins = customMinutesInput.toIntOrNull()?.coerceIn(1, 240) ?: 30
                        if (isFocusMode) {
                            viewModel.setCustomFocusMinutes(mins)
                        } else {
                            viewModel.setCustomBreakMinutes(mins)
                        }
                        showCustomMinutesDialog = false
                    }
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMinutesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Edit Daily Focus Target Dialog
    if (showDailyTargetDialog) {
        AlertDialog(
            onDismissRequest = { showDailyTargetDialog = false },
            title = {
                Text(
                    text = "Daily Focus Target",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose a daily study target in minutes:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(60 to "1h", 90 to "1.5h", 120 to "2h", 180 to "3h", 240 to "4h").forEach { (mins, label) ->
                            FilterChip(
                                selected = dailyTargetInput == mins.toString(),
                                onClick = {
                                    haptic.performClick()
                                    dailyTargetInput = mins.toString()
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = dailyTargetInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                dailyTargetInput = input
                            }
                        },
                        label = { Text("Minutes per day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_target_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performClick()
                        val mins = dailyTargetInput.toIntOrNull()?.coerceIn(15, 720) ?: 120
                        viewModel.updateDailyFocusTarget(mins)
                        showDailyTargetDialog = false
                    },
                    modifier = Modifier.testTag("save_daily_target_button")
                ) {
                    Text("Save Target")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyTargetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Smooth Apple-style Capsule Progress Bar without M3 LinearProgressIndicator stop-dot artifacts.
 */
@Composable
private fun SmoothCapsuleProgressBar(
    progress: Float,
    fillColor: Color,
    trackColor: Color,
    height: androidx.compose.ui.unit.Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val clamped by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350),
        label = "capsule_progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(50)
            )
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped.coerceAtLeast(0.03f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                fillColor.copy(alpha = 0.85f),
                                fillColor
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun DailyFocusTargetCard(
    completedMinutesToday: Int,
    dailyTargetMinutes: Int,
    remainingMinutes: Int,
    progress: Float,
    completedSessionsToday: Int,
    plannedSessionMinutes: Int,
    focusIntegrityStreak: Int,
    isRunning: Boolean,
    onEditTarget: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassCard(
                shape = RoundedCornerShape(20.dp),
                accentTint = MaterialTheme.colorScheme.primary
            )
            .testTag("daily_focus_target_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Daily Target",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${completedMinutesToday}m / ${dailyTargetMinutes}m",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .hapticClickable { onEditTarget() }
                        .testTag("edit_daily_target_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Daily Target",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${dailyTargetMinutes}m",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            SmoothCapsuleProgressBar(
                progress = progress,
                fillColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                height = 7.dp
            )
        }
    }
}
