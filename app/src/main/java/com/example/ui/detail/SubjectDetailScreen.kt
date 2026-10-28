package com.example.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.SubjectWithTopics
import com.example.data.model.Topic
import com.example.data.model.calculateSmartReadiness
import com.example.data.model.isDueForReview
import com.example.data.model.isReviewOverdue
import com.example.data.model.reviewStatusLabel
import com.example.ui.theme.appleGlassCard
import com.example.ui.theme.hapticClickable
import com.example.ui.theme.performClick
import com.example.ui.theme.performLongPress
import com.example.ui.theme.readableOnSurface
import com.example.util.DateUtils
import androidx.compose.ui.platform.LocalHapticFeedback

private fun parseSubjectColor(colorHex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubjectDetailViewModel = viewModel()
) {
    LaunchedEffect(subjectId) {
        viewModel.initialize(subjectId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(uiState.isSubjectDeleted) {
        if (uiState.isSubjectDeleted) {
            onNavigateBack()
        }
    }

    val currentSubject = uiState.subjectWithTopics?.subject
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val defaultPrimary = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentSubject != null) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(parseSubjectColor(currentSubject.colorTag, defaultPrimary))
                            )
                        }
                        Text(
                            text = currentSubject?.name ?: stringResource(R.string.subject_name),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performClick()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    if (currentSubject != null) {
                        IconButton(
                            onClick = {
                                haptic.performClick()
                                com.example.util.CalendarExportHelper.exportExamToCalendar(
                                    context = context,
                                    subject = currentSubject,
                                    topics = uiState.subjectWithTopics?.topics ?: emptyList()
                                )
                            },
                            modifier = Modifier.testTag("detail_export_calendar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Add to Google Calendar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performClick()
                                onNavigateToEdit(currentSubject.id)
                            },
                            modifier = Modifier.testTag("edit_subject_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_subject)
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performLongPress()
                                viewModel.showDeleteDialog(true)
                            },
                            modifier = Modifier.testTag("delete_subject_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_subject),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.subjectWithTopics == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.no_matching_subjects),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    val subjectWithTopics = uiState.subjectWithTopics!!
                    val rawAccentColor = parseSubjectColor(subjectWithTopics.subject.colorTag, defaultPrimary)
                    val displayAccent = rawAccentColor.readableOnSurface(isDark)
                    val readiness = uiState.readiness ?: remember(subjectWithTopics, uiState.loggedFocusMinutes) {
                        subjectWithTopics.calculateSmartReadiness(
                            actualFocusMinutes = uiState.loggedFocusMinutes
                        )
                    }
                    val dueReviewCount = remember(subjectWithTopics.topics) {
                        val now = System.currentTimeMillis()
                        subjectWithTopics.topics.count { it.isDueForReview(now) }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 700.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Hero Countdown Card
                        item {
                            CountdownHeroCard(
                                examDate = subjectWithTopics.subject.examDate,
                                startTime = subjectWithTopics.subject.startTime,
                                endTime = subjectWithTopics.subject.endTime,
                                roomNumber = subjectWithTopics.subject.roomNumber,
                                accentColor = rawAccentColor
                            )
                        }

                        // 2. Smart Exam Readiness Card (Consolidated with Syllabus & Focus stats)
                        item {
                            SmartExamReadinessCard(
                                readiness = readiness,
                                showUrgencyAlert = uiState.readinessAlertsEnabled,
                                accentColor = displayAccent
                            )
                        }

                        // 3. Exam Hall Vault Card (2x2 balanced grid + single-line actions)
                        item {
                            ExamHallVaultCard(
                                subject = subjectWithTopics.subject,
                                accentColor = rawAccentColor,
                                onEditVaultClick = { viewModel.showVaultEditDialog(true) },
                                onAttachFormulaSheet = { uri ->
                                    viewModel.attachFormulaSheet(uri)
                                },
                                onRemoveFormulaSheet = {
                                    viewModel.removeFormulaSheet()
                                }
                            )
                        }

                        // 4. Minimal Syllabus Section Header + Quick Add Topic Bar
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        Text(
                                            text = "Syllabus",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Text(
                                                text = "${subjectWithTopics.completedTopics}/${subjectWithTopics.totalTopics}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    if (dueReviewCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFD97706).copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.4f)),
                                            modifier = Modifier.testTag("due_revisions_badge")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.EventRepeat,
                                                    contentDescription = null,
                                                    tint = Color(0xFFD97706),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "$dueReviewCount Due",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFD97706),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick Add Topic Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.newTopicTitle,
                                        onValueChange = viewModel::onNewTopicTitleChange,
                                        placeholder = {
                                            Text(
                                                text = stringResource(R.string.add_topic_placeholder),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { viewModel.addTopic() }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("new_topic_input")
                                    )

                                    Button(
                                        onClick = {
                                            haptic.performLongPress()
                                            viewModel.addTopic()
                                        },
                                        enabled = uiState.newTopicTitle.isNotBlank(),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .height(54.dp)
                                            .testTag("add_topic_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add topic"
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Topic Checklist Items
                        if (subjectWithTopics.topics.isEmpty()) {
                            item {
                                EmptyTopicsCard()
                            }
                        } else {
                            itemsIndexed(
                                items = subjectWithTopics.topics,
                                key = { _, topic -> topic.id },
                                contentType = { _, _ -> "topic_item" }
                            ) { index, topic ->
                                TopicChecklistItem(
                                    topic = topic,
                                    accentColor = displayAccent,
                                    isFirst = index == 0,
                                    isLast = index == subjectWithTopics.topics.lastIndex,
                                    onToggleDone = {
                                        haptic.performLongPress()
                                        viewModel.toggleTopicDone(topic)
                                    },
                                    onMarkReviewed = {
                                        haptic.performLongPress()
                                        viewModel.markTopicReviewed(topic)
                                    },
                                    onSnoozeReview = {
                                        haptic.performClick()
                                        viewModel.snoozeTopicReview(topic)
                                    },
                                    onDelete = {
                                        haptic.performClick()
                                        viewModel.deleteTopic(topic.id)
                                    },
                                    onMoveUp = {
                                        haptic.performClick()
                                        viewModel.moveTopicUp(topic)
                                    },
                                    onMoveDown = {
                                        haptic.performClick()
                                        viewModel.moveTopicDown(topic)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showDeleteDialog && currentSubject != null) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performLongPress()
                        viewModel.deleteSubject()
                    },
                    modifier = Modifier.testTag("confirm_delete_subject_button")
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteDialog(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showVaultEditDialog && currentSubject != null) {
        EditExamHallVaultDialog(
            subject = currentSubject,
            onDismiss = { viewModel.showVaultEditDialog(false) },
            onSave = { room, building, seat, roll, hrs, links, notes ->
                viewModel.updateExamHallVault(
                    roomNumber = room,
                    examHallBuilding = building,
                    seatNumber = seat,
                    rollNumber = roll,
                    expectedStudyHours = hrs,
                    referenceLinks = links,
                    notes = notes
                )
            }
        )
    }
}

@Composable
fun CountdownHeroCard(
    examDate: Long,
    accentColor: Color,
    modifier: Modifier = Modifier,
    startTime: String = "",
    endTime: String = "",
    roomNumber: String = ""
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val displayAccent = accentColor.readableOnSurface(isDark)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .appleGlassCard(
                shape = RoundedCornerShape(22.dp),
                accentTint = accentColor
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Top Metadata Row: Date + Time + Room Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = displayAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = DateUtils.formatFullDate(examDate),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (startTime.isNotBlank() || roomNumber.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (startTime.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = displayAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (endTime.isNotBlank()) "$startTime–$endTime" else startTime,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        if (roomNumber.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = displayAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = roomNumber,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-time 4-box Digital Countdown
            com.example.ui.components.RealtimeCountdownTimer(
                examDateMillis = examDate,
                startTime = startTime,
                accentColor = displayAccent
            )
        }
    }
}

@Composable
fun ProgressOverviewCard(
    item: SubjectWithTopics,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val progressAnimated by animateFloatAsState(targetValue = item.progress, label = "detail_progress")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Syllabus Completion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${(progressAnimated * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progressAnimated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun TopicChecklistItem(
    topic: Topic,
    accentColor: Color,
    isFirst: Boolean,
    isLast: Boolean,
    onToggleDone: () -> Unit,
    onMarkReviewed: () -> Unit = {},
    onSnoozeReview: () -> Unit = {},
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isOverdue = topic.isReviewOverdue(now)
    val isDueToday = topic.isDueForReview(now)
    val statusLabel = topic.reviewStatusLabel(now)

    val textColor by animateColorAsState(
        targetValue = if (topic.isDone) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "topic_text_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("topic_item_${topic.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverdue -> Color(0xFFEF4444).copy(alpha = 0.08f)
                isDueToday -> Color(0xFFF59E0B).copy(alpha = 0.08f)
                topic.isDone -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isOverdue -> Color(0xFFEF4444).copy(alpha = 0.45f)
                isDueToday -> Color(0xFFF59E0B).copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = topic.isDone,
                    onCheckedChange = { onToggleDone() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentColor,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.testTag("topic_checkbox_${topic.id}")
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleDone() }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (topic.isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (statusLabel.isNotBlank()) {
                        val badgeColor = when {
                            isOverdue -> Color(0xFFDC2626)
                            isDueToday -> Color(0xFFD97706)
                            topic.isDone -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = "$statusLabel • Rev #${topic.revisionCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = badgeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!isFirst) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (!isLast) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_topic_${topic.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Topic",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (topic.isDone) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 42.dp, end = 8.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onMarkReviewed() }
                            .testTag("mark_reviewed_${topic.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Review Done (+1)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (isOverdue || isDueToday) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSnoozeReview() }
                                .testTag("snooze_review_${topic.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Snooze,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Snooze 1d",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTopicsCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .appleGlassCard(shape = RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.no_topics_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
