package com.example.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.ExamReadinessResult
import com.example.data.model.ExamUrgencyStatus
import com.example.data.model.Subject
import com.example.ui.theme.appleGlassCard
import com.example.ui.theme.readableOnSurface
import java.io.File
import java.util.Locale

@Composable
fun SmartExamReadinessCard(
    readiness: ExamReadinessResult,
    showUrgencyAlert: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val urgencyColor = when (readiness.urgencyStatus) {
        ExamUrgencyStatus.HIGH_URGENCY -> Color(0xFFDC2626)
        ExamUrgencyStatus.NEEDS_ATTENTION -> Color(0xFFD97706)
        ExamUrgencyStatus.ON_TRACK -> Color(0xFF0284C7)
        ExamUrgencyStatus.READY -> Color(0xFF059669)
        ExamUrgencyStatus.COMPLETED -> Color(0xFF64748B)
    }

    val animatedScoreProgress by animateFloatAsState(
        targetValue = (readiness.score / 100f).coerceIn(0f, 1f),
        label = "readiness_score_progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .appleGlassCard(
                shape = RoundedCornerShape(20.dp),
                accentTint = if (readiness.urgencyStatus == ExamUrgencyStatus.HIGH_URGENCY) urgencyColor else accentColor
            )
            .testTag("smart_readiness_detail_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Single-line Header: Title + Status Badge + Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Readiness",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = urgencyColor.copy(alpha = if (isDark) 0.18f else 0.14f),
                        border = BorderStroke(1.dp, urgencyColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = readiness.urgencyStatus.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = urgencyColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${readiness.score}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = urgencyColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.testTag("readiness_score_value")
                )
            }

            LinearProgressIndicator(
                progress = { animatedScoreProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = urgencyColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Compact Single-Line Urgency Banner when needed
            if (showUrgencyAlert && (readiness.urgencyStatus == ExamUrgencyStatus.HIGH_URGENCY || readiness.urgencyStatus == ExamUrgencyStatus.NEEDS_ATTENTION)) {
                val dailyTargetLabel = if (readiness.recommendedDailyMinutes >= 60) {
                    String.format(Locale.US, "%.1fh/day", readiness.recommendedDailyMinutes / 60f)
                } else {
                    "${readiness.recommendedDailyMinutes}m/day"
                }
                val compactAlert = when (readiness.daysRemaining) {
                    0 -> "Exam is today • Focus on key revisions"
                    1 -> "Exam tomorrow • Target $dailyTargetLabel"
                    else -> "${readiness.daysRemaining}d left • Target $dailyTargetLabel to reach 100%"
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = urgencyColor.copy(alpha = if (isDark) 0.14f else 0.10f),
                    border = BorderStroke(1.dp, urgencyColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("readiness_urgency_banner")
                ) {
                    Text(
                        text = compactAlert,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = urgencyColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // 3 Responsive Metric Pills: Syllabus, Focus, Daily Goal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadinessMetricBox(
                    label = "Syllabus",
                    value = "${readiness.completedTopics}/${readiness.totalTopics}",
                    subtext = "${readiness.remainingTopics} left",
                    modifier = Modifier.weight(1f)
                )
                ReadinessMetricBox(
                    label = "Focus",
                    value = "${readiness.actualFocusMinutes}m",
                    subtext = "Goal ${readiness.expectedFocusMinutes}m",
                    modifier = Modifier.weight(1f)
                )
                ReadinessMetricBox(
                    label = "Daily Goal",
                    value = if (readiness.recommendedDailyMinutes >= 60) {
                        String.format(Locale.US, "%.1fh/d", readiness.recommendedDailyMinutes / 60f)
                    } else {
                        "${readiness.recommendedDailyMinutes}m/d"
                    },
                    subtext = if (readiness.daysRemaining >= 0) "${readiness.daysRemaining}d left" else "Passed",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReadinessMetricBox(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ExamHallVaultCard(
    subject: Subject,
    accentColor: Color,
    onEditVaultClick: () -> Unit,
    onAttachFormulaSheet: (Uri) -> Unit,
    onRemoveFormulaSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val displayAccent = accentColor.readableOnSurface(isDark)

    val formulaSheetPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachFormulaSheet(uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .appleGlassCard(shape = RoundedCornerShape(20.dp))
            .testTag("exam_hall_vault_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Single-line Header Row (No vertical text wrapping!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = displayAccent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, displayAccent.copy(alpha = 0.35f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = displayAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Exam Hall Vault",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.75f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEditVaultClick() }
                        .testTag("edit_exam_vault_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Exam Vault",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Responsive 2x2 Grid for Building, Room, Seat, Roll / ID
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultDetailPill(
                        icon = Icons.Default.LocationCity,
                        label = "Building",
                        value = subject.examHallBuilding.ifBlank { "—" },
                        accentColor = displayAccent,
                        modifier = Modifier.weight(1f)
                    )
                    VaultDetailPill(
                        icon = Icons.Default.MeetingRoom,
                        label = "Room",
                        value = subject.roomNumber.ifBlank { "—" },
                        accentColor = displayAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultDetailPill(
                        icon = Icons.Default.Chair,
                        label = "Seat",
                        value = subject.seatNumber.ifBlank { "—" },
                        accentColor = displayAccent,
                        modifier = Modifier.weight(1f)
                    )
                    VaultDetailPill(
                        icon = Icons.Default.Badge,
                        label = "Roll / ID",
                        value = subject.rollNumber.ifBlank { "—" },
                        accentColor = displayAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Compact Single-Line Formula Sheet Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = displayAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (subject.formulaSheetUri.isNotBlank()) {
                                File(subject.formulaSheetUri).name
                            } else {
                                "Formula Sheet (PDF / Image)"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (subject.formulaSheetUri.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    try {
                                        val file = File(subject.formulaSheetUri)
                                        if (file.exists()) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val mime = if (file.name.endsWith(".pdf", true)) "application/pdf" else "image/*"
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, mime)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        }
                                    } catch (_: Exception) {
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("open_formula_sheet_button")
                            ) {
                                Text("Open", maxLines = 1, softWrap = false)
                            }
                            TextButton(
                                onClick = onRemoveFormulaSheet,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Remove", color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    formulaSheetPicker.launch(arrayOf("application/pdf", "image/*"))
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("attach_formula_sheet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Attach",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Reference Links & Exam Notes if present
            if (subject.referenceLinks.isNotBlank() || subject.notes.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (subject.referenceLinks.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable {
                                try {
                                    val link = if (subject.referenceLinks.startsWith("http")) {
                                        subject.referenceLinks
                                    } else {
                                        "https://${subject.referenceLinks}"
                                    }
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                } catch (_: Exception) {
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = subject.referenceLinks,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    if (subject.notes.isNotBlank()) {
                        Text(
                            text = subject.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultDetailPill(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EditExamHallVaultDialog(
    subject: Subject,
    onDismiss: () -> Unit,
    onSave: (
        roomNumber: String,
        examHallBuilding: String,
        seatNumber: String,
        rollNumber: String,
        expectedStudyHours: Int,
        referenceLinks: String,
        notes: String
    ) -> Unit
) {
    var roomNumber by remember { mutableStateOf(subject.roomNumber) }
    var building by remember { mutableStateOf(subject.examHallBuilding) }
    var seatNumber by remember { mutableStateOf(subject.seatNumber) }
    var rollNumber by remember { mutableStateOf(subject.rollNumber) }
    var expectedHoursInput by remember { mutableStateOf(subject.expectedStudyHours.toInt().toString()) }
    var referenceLinks by remember { mutableStateOf(subject.referenceLinks) }
    var notes by remember { mutableStateOf(subject.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Exam Hall Vault",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = building,
                        onValueChange = { building = it },
                        label = { Text("Building") },
                        placeholder = { Text("Block B") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vault_building_input")
                    )
                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("Room") },
                        placeholder = { Text("302") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vault_room_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = seatNumber,
                        onValueChange = { seatNumber = it },
                        label = { Text("Seat") },
                        placeholder = { Text("C-14") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vault_seat_input")
                    )
                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        label = { Text("Roll / ID") },
                        placeholder = { Text("2024CS108") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vault_roll_input")
                    )
                }

                OutlinedTextField(
                    value = expectedHoursInput,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) expectedHoursInput = it },
                    label = { Text("Target Study Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_expected_hours_input")
                )

                OutlinedTextField(
                    value = referenceLinks,
                    onValueChange = { referenceLinks = it },
                    label = { Text("Reference URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_links_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Exam Notes") },
                    placeholder = { Text("Bring ID & calculator") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hrs = expectedHoursInput.toIntOrNull()?.coerceIn(1, 200) ?: 8
                    onSave(roomNumber, building, seatNumber, rollNumber, hrs, referenceLinks, notes)
                },
                modifier = Modifier.testTag("save_vault_button")
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
