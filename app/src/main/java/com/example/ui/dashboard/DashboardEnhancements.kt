package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamReadinessResult
import com.example.data.model.ExamUrgencyStatus
import com.example.data.model.SubjectWithTopics
import com.example.data.model.Topic
import com.example.data.model.isReviewOverdue
import com.example.data.model.reviewStatusLabel

/**
 * Prominent Smart Exam Readiness & High Urgency Alert Banner displayed on the Dashboard
 * when an upcoming exam requires attention or has High Urgency.
 */
@Composable
fun DashboardUrgencyAlertBanner(
    exam: SubjectWithTopics,
    readiness: ExamReadinessResult,
    onStartFocusClick: (SubjectWithTopics) -> Unit,
    onExamClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHighUrgency = readiness.urgencyStatus == ExamUrgencyStatus.HIGH_URGENCY
    val accentColor = when (readiness.urgencyStatus) {
        ExamUrgencyStatus.HIGH_URGENCY -> Color(0xFFEF4444)
        ExamUrgencyStatus.NEEDS_ATTENTION -> Color(0xFFF59E0B)
        ExamUrgencyStatus.ON_TRACK -> Color(0xFF3B82F6)
        ExamUrgencyStatus.READY -> Color(0xFF10B981)
        ExamUrgencyStatus.COMPLETED -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExamClick(exam.subject.id) }
            .testTag("dashboard_urgency_alert_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isHighUrgency) Icons.Default.LocalFireDepartment else Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    val daysLabel = when (readiness.daysRemaining) {
                        0 -> "Exam today"
                        1 -> "Exam tomorrow"
                        else -> "Exam in ${readiness.daysRemaining}d"
                    }
                    Text(
                        text = "${exam.subject.name} • $daysLabel",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${readiness.urgencyStatus.label} • Target ${readiness.recommendedDailyMinutes}m today",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onStartFocusClick(exam) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("urgency_banner_focus_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Focus",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Spaced Repetition Due Queue Card displayed on the Dashboard
 * when completed topics reach their review intervals.
 */
@Composable
fun SpacedRepetitionDashboardCard(
    dueTopics: List<TopicWithSubjectInfo>,
    onMarkReviewed: (Topic) -> Unit,
    onSnoozeReview: (Topic) -> Unit,
    onOpenSubject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dueTopics.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spaced_repetition_dashboard_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventRepeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Reviews Due (${dueTopics.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            dueTopics.take(3).forEach { item ->
                val isOverdue = item.topic.isReviewOverdue()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSubject(item.subjectId) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.topic.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.subjectName} • ${item.topic.reviewStatusLabel()} (Stage ${item.topic.revisionCount + 1})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = { onSnoozeReview(item.topic) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Snooze,
                                    contentDescription = "Snooze 1 day",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("+1d", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = { onMarkReviewed(item.topic) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("dashboard_review_topic_${item.topic.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reviewed",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
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
 * Quick Action Bar on the Dashboard for scanning Admit Cards with on-device ML Kit OCR
 * or launching the full-screen Exam Focus War-Room.
 */
@Composable
fun DashboardQuickActionsBar(
    isExamModeEnabled: Boolean,
    onScanAdmitCardClick: () -> Unit,
    onOpenWarRoomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onScanAdmitCardClick() }
                .testTag("dashboard_scan_admit_card_btn")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Scan Admit Card (OCR)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isExamModeEnabled) Color(0xFFEF4444).copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            border = BorderStroke(
                1.dp,
                if (isExamModeEnabled) Color(0xFFEF4444).copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpenWarRoomClick() }
                .testTag("dashboard_open_war_room_btn")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (isExamModeEnabled) Color(0xFFEF4444) else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Focus Hub",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isExamModeEnabled) Color(0xFFEF4444) else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * Renders compact Exam Hall Vault logistics pills & Smart Readiness score inside a SubjectCardItem.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectCardSmartFooter(
    item: SubjectWithTopics,
    readiness: ExamReadinessResult,
    modifier: Modifier = Modifier
) {
    val urgencyColor = when (readiness.urgencyStatus) {
        ExamUrgencyStatus.READY -> Color(0xFF10B981)
        ExamUrgencyStatus.ON_TRACK -> Color(0xFF3B82F6)
        ExamUrgencyStatus.NEEDS_ATTENTION -> Color(0xFFF59E0B)
        ExamUrgencyStatus.HIGH_URGENCY -> Color(0xFFEF4444)
        ExamUrgencyStatus.COMPLETED -> MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Smart Exam Readiness Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = urgencyColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "Readiness ${readiness.score}/100 • ${readiness.urgencyStatus.label}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = urgencyColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (readiness.dueReviewCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "${readiness.dueReviewCount} review due",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            val loggedHours = readiness.loggedMinutes / 60
            val loggedRemMins = readiness.loggedMinutes % 60
            val loggedLabel = if (loggedHours > 0) "${loggedHours}h ${loggedRemMins}m" else "${loggedRemMins}m"
            Text(
                text = "Focus: $loggedLabel / ${item.subject.expectedStudyHours.toInt()}h",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Exam Hall Vault Pills (if any logistics configured)
        val hasLogistics = item.subject.roomLocation.isNotBlank() ||
            item.subject.examHallBuilding.isNotBlank() ||
            item.subject.seatNumber.isNotBlank()
        if (hasLogistics) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.subject.examHallBuilding.isNotBlank()) {
                    LogisticsMiniTag(icon = Icons.Default.LocationCity, text = item.subject.examHallBuilding)
                }
                if (item.subject.roomLocation.isNotBlank()) {
                    LogisticsMiniTag(icon = Icons.Default.MeetingRoom, text = item.subject.roomLocation)
                }
                if (item.subject.seatNumber.isNotBlank()) {
                    LogisticsMiniTag(icon = Icons.Default.Chair, text = "Seat ${item.subject.seatNumber}")
                }
            }
        }
    }
}

@Composable
private fun LogisticsMiniTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
