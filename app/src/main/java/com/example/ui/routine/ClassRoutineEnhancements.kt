package com.example.ui.routine

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceMarginStatus
import com.example.data.model.AttendanceSafeMarginInfo
import com.example.data.model.ClassRoutineItem
import com.example.util.TimetableExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CampusGapFocusCard(
    gap: RoutineGap,
    onStartGapSession: (durationMinutes: Int, subjectId: String?, subjectName: String, subjectColor: String) -> Unit,
    onDismissGap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!gap.canFitStudySession || gap.isDismissed) {
        // Compact non-intrusive break banner
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag("routine_gap_compact_${gap.index}"),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Coffee,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = gap.timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!gap.canFitStudySession) {
                    Text(
                        text = "Short Walk Break",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val subjectColor = remember(gap.recommendedSubjectColor) {
        try {
            Color(android.graphics.Color.parseColor(gap.recommendedSubjectColor))
        } catch (e: Exception) {
            Color(0xFF0D9488)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("campus_gap_focus_card_${gap.index}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = subjectColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                    Surface(
                        shape = CircleShape,
                        color = subjectColor.copy(alpha = 0.18f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = subjectColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${gap.durationMinutes}m Break • ${gap.recommendedSubjectName}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = subjectColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = gap.timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            onStartGapSession(
                                gap.suggestedPomodoroMinutes,
                                gap.recommendedSubjectId,
                                gap.recommendedSubjectName,
                                gap.recommendedSubjectColor
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = subjectColor),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("start_gap_focus_button_${gap.index}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${gap.suggestedPomodoroMinutes}m Focus",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(
                        onClick = { onDismissGap(gap.gapId) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("dismiss_gap_button_${gap.index}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss gap suggestion",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceSafeMarginSection(
    routine: ClassRoutineItem,
    safeMargin: AttendanceSafeMarginInfo,
    onRecordAttendance: (attended: Boolean) -> Unit,
    onEditAttendanceCounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (safeMargin.status) {
        com.example.data.model.AttendanceSafeMarginStatus.RECOVERY_NEEDED -> Color(0xFFDC2626)
        com.example.data.model.AttendanceSafeMarginStatus.ON_BOUNDARY -> Color(0xFFD97706)
        com.example.data.model.AttendanceSafeMarginStatus.SAFE_SKIPPABLE -> Color(0xFF059669)
        com.example.data.model.AttendanceSafeMarginStatus.NO_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = 1.dp,
            color = if (safeMargin.isBelowThreshold) {
                Color(0xFFEF4444).copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_safe_margin_${routine.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Attendance:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${safeMargin.currentPercentage}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                    Text(
                        text = "(${safeMargin.attended}/${safeMargin.totalConducted} • Missed ${safeMargin.missed})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onEditAttendanceCounts,
                        modifier = Modifier
                            .size(22.dp)
                            .testTag("edit_attendance_counts_${routine.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit attendance counts",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRecordAttendance(true) }
                            .testTag("btn_attend_${routine.id}")
                    ) {
                        Text(
                            text = "+ Attended",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRecordAttendance(false) }
                            .testTag("btn_miss_${routine.id}")
                    ) {
                        Text(
                            text = "✕ Missed",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (safeMargin.totalConducted > 0) {
                LinearProgressIndicator(
                    progress = { (safeMargin.currentPercentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // 75% Safe-Margin Headline & Actionable Guidance
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = safeMargin.headline,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                    Text(
                        text = safeMargin.explanation,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EditAttendanceDialog(
    routine: ClassRoutineItem,
    onDismiss: () -> Unit,
    onSave: (attended: Int, missed: Int) -> Unit,
    thresholdPercent: Int = 80
) {
    var attendedInput by remember { mutableStateOf(routine.attendedCount.toString()) }
    var missedInput by remember { mutableStateOf(routine.missedCount.toString()) }

    val previewAttended = attendedInput.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val previewMissed = missedInput.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val previewMargin = remember(previewAttended, previewMissed, thresholdPercent) {
        routine.copy(attendedCount = previewAttended, missedCount = previewMissed)
            .calculateSafeMargin(thresholdPercent)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$thresholdPercent% Attendance Tracker",
                    fontWeight = FontWeight.Bold
                )
                if (previewAttended > 0 || previewMissed > 0) {
                    TextButton(
                        onClick = {
                            attendedInput = "0"
                            missedInput = "0"
                        },
                        modifier = Modifier.testTag("reset_attendance_counts_button")
                    ) {
                        Text("Reset")
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = routine.subjectName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = attendedInput,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) attendedInput = it },
                        label = { Text("Present") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_attended_count")
                    )
                    OutlinedTextField(
                        value = missedInput,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) missedInput = it },
                        label = { Text("Absent") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_missed_count")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Total Classes: ${previewMargin.totalConducted} • Current: ${previewMargin.currentPercentage}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = previewMargin.headline,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(previewAttended, previewMissed)
                },
                modifier = Modifier.testTag("save_attendance_counts_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimetableExportDialog(
    routines: List<ClassRoutineItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var compactLockScreen by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(routines, compactLockScreen) {
        withContext(Dispatchers.Default) {
            val bmp = TimetableExporter.renderTimetableBitmap(
                routines = routines,
                compactLockScreenFormat = compactLockScreen
            )
            previewBitmap = bmp
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Timetable",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Format selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !compactLockScreen,
                        onClick = { compactLockScreen = false },
                        label = { Text("Weekly Grid", maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("format_weekly_grid_chip")
                    )
                    FilterChip(
                        selected = compactLockScreen,
                        onClick = { compactLockScreen = true },
                        label = { Text("Wallpaper", maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("format_lockscreen_chip")
                    )
                }

                // Live Preview
                previewBitmap?.let { bmp ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Timetable export preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF06080C))
                        )
                    }
                }

                if (statusMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = statusMessage ?: "",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                }

                // Export Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isExporting = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    TimetableExporter.exportAsPng(
                                        context = context,
                                        routines = routines,
                                        compactLockScreen = compactLockScreen
                                    )
                                }
                                isExporting = false
                                result.onSuccess { res ->
                                    statusMessage = "Saved & opened Share sheet"
                                    TimetableExporter.shareExportedFile(context, res)
                                }.onFailure { err ->
                                    statusMessage = "Export failed: ${err.localizedMessage ?: "Unknown error"}"
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_png_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (compactLockScreen) "Share Wallpaper" else "Share Weekly Grid")
                    }

                    OutlinedButton(
                        onClick = {
                            isExporting = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    TimetableExporter.exportAsPdf(
                                        context = context,
                                        routines = routines
                                    )
                                }
                                isExporting = false
                                result.onSuccess { res ->
                                    statusMessage = "Saved PDF & opened Share sheet"
                                    TimetableExporter.shareExportedFile(context, res)
                                }.onFailure { err ->
                                    statusMessage = "PDF export failed: ${err.localizedMessage ?: "Unknown error"}"
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_pdf_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export PDF")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
