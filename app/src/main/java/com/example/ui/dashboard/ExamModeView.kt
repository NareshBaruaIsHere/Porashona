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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamReadinessResult
import com.example.data.model.ExamUrgencyStatus
import com.example.data.model.SubjectWithTopics
import com.example.data.model.Topic
import com.example.data.model.calculateSmartReadiness
import com.example.data.model.isDueForReview
import com.example.data.model.isReviewOverdue
import com.example.data.model.reviewStatusLabel
import com.example.ui.components.RealtimeCountdownTimer
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamModeView(
    activeExam: SubjectWithTopics?,
    allUpcomingExams: List<SubjectWithTopics>,
    onSelectExam: (SubjectWithTopics) -> Unit,
    onToggleTopic: (Topic) -> Unit,
    onAddTopic: (subjectId: String, title: String) -> Unit,
    onStartFocus: (subjectName: String) -> Unit,
    onExitExamMode: () -> Unit,
    onNavigateToSubjectDetail: (String) -> Unit,
    studyMinutesForSubject: Int = 0,
    completedFocusMinutesToday: Int = 0,
    dailyFocusTargetMinutes: Int = 120,
    readinessAlertsEnabled: Boolean = true,
    onMarkTopicReviewed: (Topic) -> Unit = {},
    onSnoozeTopicReview: (Topic) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (activeExam == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Text(
                    text = "No Upcoming Exams Found",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add an exam to your schedule to activate full Exam Focus Mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onExitExamMode) {
                    Text("Return to Dashboard")
                }
            }
        }
        return
    }

    val subject = activeExam.subject
    val accentColor = remember(subject.colorTag) {
        try {
            Color(android.graphics.Color.parseColor(subject.colorTag))
        } catch (e: Exception) {
            Color(0xFF0D9488)
        }
    }

    val smartReadiness: ExamReadinessResult = remember(activeExam, studyMinutesForSubject) {
        activeExam.calculateSmartReadiness(studyMinutesForSubject)
    }

    val urgencyColor = when (smartReadiness.urgencyStatus) {
        ExamUrgencyStatus.HIGH_URGENCY -> Color(0xFFDC2626)
        ExamUrgencyStatus.NEEDS_ATTENTION -> Color(0xFFD97706)
        ExamUrgencyStatus.ON_TRACK -> Color(0xFF0284C7)
        ExamUrgencyStatus.READY -> Color(0xFF059669)
        ExamUrgencyStatus.COMPLETED -> Color(0xFF6B7280)
    }

    var newTopicTitle by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Exam Mode Banner & Daily Focus Progress
        item(key = "exam_mode_banner") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exam_mode_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "EXAM FOCUS MODE ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.1.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFDC2626)
                                )
                                Text(
                                    text = "Today's Focus: ${completedFocusMinutesToday}m / ${dailyFocusTargetMinutes}m goal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onExitExamMode,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("exit_exam_mode_button")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    val dailyProgress = if (dailyFocusTargetMinutes > 0) {
                        (completedFocusMinutesToday.toFloat() / dailyFocusTargetMinutes.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { dailyProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFEF4444),
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }

        // 2. Exam Selector if multiple upcoming exams exist
        if (allUpcomingExams.size > 1) {
            item(key = "exam_selector_row") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allUpcomingExams) { item ->
                        val isSelected = item.subject.id == subject.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectExam(item) },
                            label = {
                                Text(
                                    item.subject.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            try {
                                                Color(android.graphics.Color.parseColor(item.subject.colorTag))
                                            } catch (e: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // 3. Hero Active Exam Countdown & Exam Hall Vault Quick-Access Card
        item(key = "hero_active_countdown") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = accentColor.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor,
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    RealtimeCountdownTimer(
                        examDateMillis = subject.examDate,
                        startTime = subject.startTime,
                        accentColor = accentColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Exam Date, Time & Exam Hall Vault Pills
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = DateUtils.formatFullDate(subject.examDate),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (subject.startTime.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = if (subject.endTime.isNotBlank()) "${subject.startTime} – ${subject.endTime}" else subject.startTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (subject.examHallBuilding.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.LocationCity, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = subject.examHallBuilding,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (subject.roomNumber.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.MeetingRoom, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = subject.roomNumber,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (subject.seatNumber.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Chair, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "Seat ${subject.seatNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (subject.rollNumber.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "ID: ${subject.rollNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Smart Exam Readiness Index & Urgency Alert Score Card
        item(key = "exam_readiness_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exam_mode_readiness_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (smartReadiness.urgencyStatus) {
                        ExamUrgencyStatus.HIGH_URGENCY -> Color(0xFFEF4444).copy(alpha = 0.08f)
                        ExamUrgencyStatus.NEEDS_ATTENTION -> Color(0xFFF59E0B).copy(alpha = 0.08f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = urgencyColor.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exam Readiness Index (0–100)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Logged ${smartReadiness.actualFocusMinutes}m / ${smartReadiness.expectedFocusMinutes}m • ${smartReadiness.completedTopics}/${smartReadiness.totalTopics} topics",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = urgencyColor
                        ) {
                            Text(
                                text = "${smartReadiness.score}/100 • ${smartReadiness.urgencyStatus.label}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (smartReadiness.score / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = urgencyColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    if (readinessAlertsEnabled) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = urgencyColor.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val headline = when {
                                smartReadiness.urgencyStatus == ExamUrgencyStatus.HIGH_URGENCY && smartReadiness.daysRemaining == 0 ->
                                    "High Urgency • Exam is today"
                                smartReadiness.urgencyStatus == ExamUrgencyStatus.HIGH_URGENCY ->
                                    "High Urgency • Exam in ${smartReadiness.daysRemaining}d"
                                smartReadiness.urgencyStatus == ExamUrgencyStatus.NEEDS_ATTENTION ->
                                    "Needs Attention • ${smartReadiness.daysRemaining}d left"
                                else ->
                                    "${smartReadiness.urgencyStatus.label} • ${smartReadiness.daysRemaining}d left"
                            }
                            Text(
                                text = "$headline • ${smartReadiness.recommendedActionText}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = urgencyColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Syllabus Topics Checklist & Spaced Repetition Priority Queue
        item(key = "syllabus_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Priority Syllabus & Due Revisions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${activeExam.topics.count { !it.isDone }} incomplete",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Order: Incomplete topics first, then overdue/due spaced repetition topics, then completed
        val now = System.currentTimeMillis()
        val sortedTopics = activeExam.topics.sortedWith(
            compareBy<Topic> {
                when {
                    !it.isDone -> 0
                    it.isReviewOverdue(now) -> 1
                    it.isDueForReview(now) -> 2
                    else -> 3
                }
            }.thenBy { it.order }
        )

        if (sortedTopics.isEmpty()) {
            item(key = "no_topics_in_exam_mode") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No syllabus topics added yet. Add key topics below to track them!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(sortedTopics, key = { it.id }) { topic ->
                val isOverdue = topic.isReviewOverdue(now)
                val isDueToday = topic.isDueForReview(now)
                val reviewLabel = topic.reviewStatusLabel(now)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTopic(topic) }
                        .testTag("exam_mode_topic_${topic.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isOverdue -> Color(0xFFEF4444).copy(alpha = 0.08f)
                            isDueToday -> Color(0xFFF59E0B).copy(alpha = 0.08f)
                            topic.isDone -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        when {
                            isOverdue -> Color(0xFFEF4444).copy(alpha = 0.4f)
                            isDueToday -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                            topic.isDone -> Color.Transparent
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = topic.isDone,
                            onCheckedChange = { onToggleTopic(topic) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = accentColor,
                                uncheckedColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topic.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (topic.isDone) FontWeight.Normal else FontWeight.Medium,
                                textDecoration = if (topic.isDone && !isOverdue && !isDueToday) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (topic.isDone && !isOverdue && !isDueToday) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (reviewLabel.isNotBlank()) {
                                Text(
                                    text = "$reviewLabel • Rev #${topic.revisionCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isOverdue) Color(0xFFDC2626) else if (isDueToday) Color(0xFFD97706) else Color(0xFF7C3AED)
                                )
                            }
                        }

                        if (topic.isDone) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onMarkTopicReviewed(topic) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EventRepeat,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Revise",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF7C3AED)
                                    )
                                }
                            }

                            if (isOverdue || isDueToday) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSnoozeTopicReview(topic) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Snooze,
                                        contentDescription = "Snooze 1 day",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(5.dp)
                                            .size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fast Add Topic Input
        item(key = "fast_add_topic_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTopicTitle,
                    onValueChange = { newTopicTitle = it },
                    placeholder = { Text("Add topic for today's review…") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("exam_mode_new_topic_input"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newTopicTitle.isNotBlank()) {
                                onAddTopic(subject.id, newTopicTitle)
                                newTopicTitle = ""
                            }
                        }
                    )
                )

                Button(
                    onClick = {
                        if (newTopicTitle.isNotBlank()) {
                            onAddTopic(subject.id, newTopicTitle)
                            newTopicTitle = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("exam_mode_add_topic_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }
        }

        // 6. Action: Launch Pomodoro Focus for this Exam
        item(key = "start_exam_pomodoro") {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = { onStartFocus(subject.name) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("exam_mode_focus_button")
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Focus Session on ${subject.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // 7. Full Details Button
        item(key = "view_full_details") {
            TextButton(
                onClick = { onNavigateToSubjectDetail(subject.id) },
                modifier = Modifier.testTag("exam_mode_full_detail_btn")
            ) {
                Text(
                    text = "Open Full Subject Details & Exam Hall Vault",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
