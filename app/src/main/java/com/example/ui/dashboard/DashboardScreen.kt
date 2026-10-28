package com.example.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.ExamReadinessResult
import com.example.data.model.ExamUrgencyStatus
import com.example.data.model.SubjectWithTopics
import com.example.data.model.Topic
import com.example.data.model.calculateSmartReadiness
import com.example.ui.components.AdmitCardOcrScannerDialog
import com.example.ui.pomodoro.PomodoroScreen
import com.example.ui.pomodoro.PomodoroViewModel
import com.example.ui.routine.ClassRoutineScreen
import com.example.ui.study.StudyTrackerScreen
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.ui.theme.appleGlassCard
import com.example.ui.theme.appleGlassDock
import com.example.ui.theme.appleGlassPill
import com.example.ui.theme.hapticClickable
import com.example.ui.theme.performClick
import com.example.ui.theme.performLongPress
import com.example.ui.theme.readableOnSurface
import com.example.util.CountdownStatus
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainAppTab {
    EXAMS,
    TIMETABLE,
    STUDY_TRACKER,
    POMODORO
}

enum class ExamFilter {
    ALL,
    UPCOMING,
    URGENT,
    COMPLETED
}

enum class ExamSortOrder {
    NEAREST_DATE,
    LOWEST_PROGRESS,
    NAME_ASC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddSubject: () -> Unit,
    onNavigateToEditSubject: (String) -> Unit = {},
    onNavigateToSubjectDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
    pomodoroViewModel: PomodoroViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pomodoroUiState by pomodoroViewModel.uiState.collectAsStateWithLifecycle()

    var subjectToDelete by remember { mutableStateOf<SubjectWithTopics?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(MainAppTab.EXAMS) }
    val isLockdownActive = pomodoroUiState.isLockdownActive && selectedTab == MainAppTab.POMODORO
    var showAdmitCardScanner by remember { mutableStateOf(false) }
    var showQuickSheet by remember { mutableStateOf(false) }
    var routineWeekGridMode by rememberSaveable { mutableStateOf(false) }
    var externalAddClassTrigger by remember { mutableIntStateOf(0) }
    var externalOcrRoutineTrigger by remember { mutableIntStateOf(0) }
    var externalExportRoutineTrigger by remember { mutableIntStateOf(0) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(ExamFilter.ALL) }
    var selectedSort by rememberSaveable { mutableStateOf(ExamSortOrder.NEAREST_DATE) }

    val filteredSubjects = remember(uiState.subjects, uiState.subjectReadinessMap, searchQuery, selectedFilter, selectedSort) {
        val base = uiState.subjects.filter { swt ->
            val matchesQuery = searchQuery.isBlank() ||
                swt.subject.name.contains(searchQuery, ignoreCase = true) ||
                swt.subject.roomNumber.contains(searchQuery, ignoreCase = true) ||
                swt.topics.any { it.title.contains(searchQuery, ignoreCase = true) }

            val status = DateUtils.getCountdownInfo(swt.subject.examDate, startTime = swt.subject.startTime).status
            val readiness = uiState.getReadiness(swt)
            val matchesFilter = when (selectedFilter) {
                ExamFilter.ALL -> true
                ExamFilter.UPCOMING -> status == CountdownStatus.UPCOMING || status == CountdownStatus.URGENT || status == CountdownStatus.TODAY
                ExamFilter.URGENT -> status == CountdownStatus.URGENT || status == CountdownStatus.TODAY ||
                    readiness.urgencyStatus == ExamUrgencyStatus.HIGH_URGENCY ||
                    readiness.urgencyStatus == ExamUrgencyStatus.NEEDS_ATTENTION
                ExamFilter.COMPLETED -> status == CountdownStatus.PASSED
            }
            matchesQuery && matchesFilter
        }

        when (selectedSort) {
            ExamSortOrder.NEAREST_DATE -> base
            ExamSortOrder.LOWEST_PROGRESS -> base.sortedBy { uiState.getReadiness(it).score }
            ExamSortOrder.NAME_ASC -> base.sortedBy { it.subject.name.lowercase() }
        }
    }

    BackHandler(enabled = selectedTab != MainAppTab.EXAMS && !isLockdownActive) {
        selectedTab = MainAppTab.EXAMS
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isLockdownActive) Color(0xFF030305) else MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (!isLockdownActive) {
                PorashonaMinimalTopBar(
                    selectedTab = selectedTab,
                    upcomingExamsCount = uiState.upcomingCount,
                    onOpenQuickSheet = { showQuickSheet = true },
                    onAddExamClick = onNavigateToAddSubject
                )
            }
        },
        bottomBar = {
            if (!isLockdownActive) {
                PorashonaFloatingGlassDock(
                    selectedTab = selectedTab,
                    onSelectTab = { tab -> selectedTab = tab }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainAppTab.EXAMS -> {
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 640.dp)
                                    .testTag("subjects_list"),
                                contentPadding = PaddingValues(
                                    start = 18.dp,
                                    end = 18.dp,
                                    top = 8.dp,
                                    bottom = 24.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Search Bar + Filter Pills (shown when subjects exist or filter/search is active)
                                if (uiState.totalSubjects > 0 || searchQuery.isNotEmpty() || selectedFilter != ExamFilter.ALL) {
                                    item(key = "search_and_filters") {
                                        SearchAndFilterBar(
                                            searchQuery = searchQuery,
                                            selectedFilter = selectedFilter,
                                            selectedSort = selectedSort,
                                            onSearchChange = { searchQuery = it },
                                            onFilterChange = { selectedFilter = it },
                                            onSortChange = { selectedSort = it },
                                            onScanAdmitCard = { showAdmitCardScanner = true }
                                        )
                                    }
                                }

                                // High-Urgency Exam Readiness Alert Banner (only if urgent exam exists and alerts enabled)
                                val mostUrgentAlert = uiState.highUrgencyExams.firstOrNull()
                                if (uiState.readinessAlertsEnabled && mostUrgentAlert != null) {
                                    val (urgentSwt, urgentReadiness) = mostUrgentAlert
                                    item(key = "urgent_exam_banner") {
                                        DashboardUrgencyAlertBanner(
                                            exam = urgentSwt,
                                            readiness = urgentReadiness,
                                            onStartFocusClick = { examSwt ->
                                                pomodoroViewModel.selectSubject(examSwt.subject)
                                                selectedTab = MainAppTab.POMODORO
                                            },
                                            onExamClick = { subjectId ->
                                                onNavigateToSubjectDetail(subjectId)
                                            }
                                        )
                                    }
                                }

                                // Ebbinghaus Spaced Repetition Due Queue (only when topics are due for review)
                                if (uiState.dueRevisionItems.isNotEmpty()) {
                                    item(key = "spaced_repetition_queue") {
                                        SpacedRepetitionDashboardCard(
                                            dueTopics = uiState.dueRevisionItems,
                                            onMarkReviewed = { topic -> viewModel.markTopicReviewed(topic) },
                                            onSnoozeReview = { topic -> viewModel.snoozeTopicReview(topic) },
                                            onOpenSubject = { subjectId -> onNavigateToSubjectDetail(subjectId) }
                                        )
                                    }
                                }

                                if (filteredSubjects.isEmpty()) {
                                    item(key = "empty_exams_view") {
                                        EmptyDashboardView(
                                            isFiltered = uiState.totalSubjects > 0,
                                            onAddSubjectClick = onNavigateToAddSubject,
                                            onScanAdmitCardClick = { showAdmitCardScanner = true },
                                            onClearFilters = {
                                                searchQuery = ""
                                                selectedFilter = ExamFilter.ALL
                                            }
                                        )
                                    }
                                } else {
                                    items(
                                        items = filteredSubjects,
                                        key = { it.subject.id },
                                        contentType = { "subject_card" }
                                    ) { item ->
                                        val readiness = uiState.getReadiness(item)
                                        SubjectCardItem(
                                            item = item,
                                            readiness = readiness,
                                            onClick = { onNavigateToSubjectDetail(item.subject.id) },
                                            onEdit = { onNavigateToEditSubject(item.subject.id) },
                                            onDelete = { subjectToDelete = item },
                                            onToggleTopic = { topic -> viewModel.toggleTopic(topic) },
                                            onStartFocus = {
                                                pomodoroViewModel.selectSubject(item.subject)
                                                selectedTab = MainAppTab.POMODORO
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                MainAppTab.TIMETABLE -> {
                    ClassRoutineScreen(
                        onNavigateToPomodoro = { subjectName ->
                            val found = uiState.subjects.firstOrNull {
                                it.subject.name.equals(subjectName, ignoreCase = true)
                            }?.subject
                            if (found != null) {
                                pomodoroViewModel.selectSubject(found)
                            }
                            selectedTab = MainAppTab.POMODORO
                        },
                        onStartGapFocusSession = { durationMinutes, subjectId, subjectName, subjectColor ->
                            pomodoroViewModel.launchQuickGapSession(
                                durationMinutes = durationMinutes,
                                subjectId = subjectId,
                                subjectName = subjectName,
                                subjectColor = subjectColor
                            )
                            selectedTab = MainAppTab.POMODORO
                        },
                        initialWeekGridMode = routineWeekGridMode,
                        externalAddClassTrigger = externalAddClassTrigger,
                        externalOcrTrigger = externalOcrRoutineTrigger,
                        externalExportTrigger = externalExportRoutineTrigger,
                        onExternalAddConsumed = { externalAddClassTrigger = 0 },
                        onExternalOcrConsumed = { externalOcrRoutineTrigger = 0 },
                        onExternalExportConsumed = { externalExportRoutineTrigger = 0 }
                    )
                }

                MainAppTab.STUDY_TRACKER -> {
                    StudyTrackerScreen(
                        onNavigateToPomodoro = {
                            selectedTab = MainAppTab.POMODORO
                        }
                    )
                }

                MainAppTab.POMODORO -> {
                    PomodoroScreen(
                        viewModel = pomodoroViewModel
                    )
                }
            }
        }
    }

    // Bottom Settings & Quick Tools Sheet
    if (showQuickSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            PorashonaQuickBottomSheet(
                totalExamsCount = uiState.totalSubjects,
                averageReadiness = uiState.averageReadinessScore,
                onOpenFullSettings = {
                    showQuickSheet = false
                    onNavigateToSettings()
                },
                onScanAdmitCard = {
                    showQuickSheet = false
                    showAdmitCardScanner = true
                },
                onScanRoutineOcr = {
                    showQuickSheet = false
                    selectedTab = MainAppTab.TIMETABLE
                    externalOcrRoutineTrigger++
                },
                onShareRoutine = {
                    showQuickSheet = false
                    selectedTab = MainAppTab.TIMETABLE
                    externalExportRoutineTrigger++
                }
            )
        }
    }

    if (showAdmitCardScanner) {
        AdmitCardOcrScannerDialog(
            existingSubjects = uiState.subjects.map { it.subject.name },
            onDismiss = { showAdmitCardScanner = false },
            onConfirmImport = { candidates ->
                viewModel.importScannedExams(candidates)
                showAdmitCardScanner = false
            }
        )
    }

    if (subjectToDelete != null) {
        val target = subjectToDelete!!
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text(text = stringResource(R.string.delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.delete_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(target.subject)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { subjectToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Minimal Top Header with Apple-style Glass Brand Pill (no gear icon, no Exam Mode pill).
 */
@Composable
private fun PorashonaMinimalTopBar(
    selectedTab: MainAppTab,
    upcomingExamsCount: Int,
    onOpenQuickSheet: () -> Unit,
    onAddExamClick: () -> Unit
) {
    val formattedDate = remember {
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Tappable Porashona Glass Brand Pill (opens Bottom Settings & Quick Tools Sheet)
        Box(
            modifier = Modifier
                .appleGlassPill(shape = RoundedCornerShape(18.dp))
                .hapticClickable { onOpenQuickSheet() }
                .testTag("settings_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color(0xFF1A5080), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Menu & Settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Right side: Compact date/upcoming pill + single '+' button ONLY on Exams tab
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.appleGlassPill(shape = RoundedCornerShape(14.dp))
            ) {
                Text(
                    text = if (selectedTab == MainAppTab.EXAMS && upcomingExamsCount > 0) {
                        "$upcomingExamsCount upcoming"
                    } else {
                        formattedDate
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            if (selectedTab == MainAppTab.EXAMS) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .hapticClickable(hapticType = HapticFeedbackType.LongPress) { onAddExamClick() }
                        .testTag("add_subject_fab")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_subject),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Apple-Style Translucent Glass Bottom Dock ("under tab navigation bar").
 * Properly respects Android 15 navigationBar insets while floating with a luminous specular border.
 */
@Composable
private fun PorashonaFloatingGlassDock(
    selectedTab: MainAppTab,
    onSelectTab: (MainAppTab) -> Unit
) {
    val tabs = listOf(
        Triple(MainAppTab.TIMETABLE, "Routine", Icons.Default.CalendarMonth to Icons.Outlined.CalendarMonth),
        Triple(MainAppTab.EXAMS, "Exams", Icons.Default.School to Icons.Outlined.School),
        Triple(MainAppTab.POMODORO, "Focus", Icons.Default.Timer to Icons.Outlined.Timer),
        Triple(MainAppTab.STUDY_TRACKER, "Stats", Icons.Default.Analytics to Icons.Outlined.Analytics)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .height(64.dp)
                .appleGlassDock(shape = RoundedCornerShape(28.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { (tab, label, icons) ->
                    val isSelected = selectedTab == tab
                    val activeAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "dock_tab_alpha"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "dock_tab_fg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f * activeAlpha),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f * activeAlpha)
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .hapticClickable { onSelectTab(tab) }
                            .testTag("tab_${tab.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) icons.first else icons.second,
                                contentDescription = label,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = contentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean Bottom Sheet for Preferences & Quick OCR/Export Actions.
 */
@Composable
private fun PorashonaQuickBottomSheet(
    totalExamsCount: Int,
    averageReadiness: Int,
    onOpenFullSettings: () -> Unit,
    onScanAdmitCard: () -> Unit,
    onScanRoutineOcr: () -> Unit,
    onShareRoutine: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1A5080), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$totalExamsCount exams • $averageReadiness% readiness",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        QuickSheetRow(
            icon = Icons.Default.Tune,
            title = "Preferences & Appearance",
            onClick = onOpenFullSettings
        )
        QuickSheetRow(
            icon = Icons.Default.HistoryEdu,
            title = "Scan Admit Card (OCR)",
            onClick = onScanAdmitCard
        )
        QuickSheetRow(
            icon = Icons.Default.DocumentScanner,
            title = "Import Class Routine (OCR)",
            onClick = onScanRoutineOcr
        )
        QuickSheetRow(
            icon = Icons.Default.IosShare,
            title = "Share Weekly Schedule",
            onClick = onShareRoutine
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun QuickSheetRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .appleGlassCard(shape = RoundedCornerShape(16.dp))
            .hapticClickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Clean, Uncongested Search & Filter Section (without Exam Mode / War Room clutter).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    selectedFilter: ExamFilter,
    selectedSort: ExamSortOrder,
    onSearchChange: (String) -> Unit,
    onFilterChange: (ExamFilter) -> Unit,
    onSortChange: (ExamSortOrder) -> Unit,
    onScanAdmitCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_subjects_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf(
                ExamFilter.ALL to stringResource(R.string.filter_all),
                ExamFilter.UPCOMING to stringResource(R.string.filter_upcoming),
                ExamFilter.URGENT to stringResource(R.string.filter_urgent),
                ExamFilter.COMPLETED to stringResource(R.string.filter_completed)
            )
            val haptic = LocalHapticFeedback.current
            filters.forEach { (filter, label) ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = {
                        haptic.performClick()
                        onFilterChange(filter)
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                )
            }

            // Compact OCR Glass Chip
            Box(
                modifier = Modifier
                    .appleGlassPill(shape = RoundedCornerShape(12.dp))
                    .hapticClickable { onScanAdmitCard() }
                    .testTag("scan_admit_card_quick_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "OCR",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            val sorts = listOf(
                ExamSortOrder.NEAREST_DATE to stringResource(R.string.sort_nearest),
                ExamSortOrder.LOWEST_PROGRESS to stringResource(R.string.sort_progress),
                ExamSortOrder.NAME_ASC to stringResource(R.string.sort_name)
            )
            sorts.forEach { (sort, label) ->
                val isSelected = selectedSort == sort
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptic.performClick()
                        onSortChange(sort)
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("sort_chip_${sort.name.lowercase()}")
                )
            }
        }
    }
}

/**
 * Lightweight Apple-style Glassmorphism Subject / Exam Card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectCardItem(
    item: SubjectWithTopics,
    readiness: ExamReadinessResult? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleTopic: (Topic) -> Unit,
    onStartFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val subject = item.subject
    val bgColor = MaterialTheme.colorScheme.background
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val isDark = remember(bgColor) { bgColor.luminance() < 0.5f }
    val rawColorTag = remember(subject.colorTag, defaultPrimary) {
        try {
            Color(android.graphics.Color.parseColor(subject.colorTag))
        } catch (e: Exception) {
            defaultPrimary
        }
    }
    val colorTag = remember(rawColorTag, isDark) { rawColorTag.readableOnSurface(isDark) }

    val countdownInfo = remember(subject.examDate, subject.startTime) {
        DateUtils.getCountdownInfo(subject.examDate, startTime = subject.startTime)
    }
    val daysRemaining = countdownInfo.daysRemaining
    val effectiveReadiness = readiness ?: remember(item) { item.calculateSmartReadiness() }
    val animatedReadiness by animateFloatAsState(
        targetValue = (effectiveReadiness.score / 100f).coerceIn(0f, 1f),
        label = "readiness_progress"
    )

    var showTopicsInline by rememberSaveable { mutableStateOf(false) }

    val countdownShort = remember(daysRemaining) {
        when {
            daysRemaining < 0 -> "Done"
            daysRemaining == 0 -> "Today"
            else -> "${daysRemaining}d"
        }
    }

    val urgencyColor = when (effectiveReadiness.urgencyStatus) {
        ExamUrgencyStatus.READY -> Color(0xFF059669)
        ExamUrgencyStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
        ExamUrgencyStatus.NEEDS_ATTENTION -> Color(0xFFD97706)
        ExamUrgencyStatus.HIGH_URGENCY -> Color(0xFFDC2626)
        ExamUrgencyStatus.COMPLETED -> MaterialTheme.colorScheme.outline
    }

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .appleGlassCard(
                shape = RoundedCornerShape(22.dp),
                accentTint = if (effectiveReadiness.urgencyStatus == ExamUrgencyStatus.HIGH_URGENCY) {
                    Color(0xFFEF4444)
                } else {
                    rawColorTag
                }
            )
            .hapticClickable(
                onLongClick = onEdit,
                onClick = onClick
            )
            .testTag("subject_card_${subject.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorTag.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, colorTag.copy(alpha = 0.45f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = countdownShort,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = if (countdownShort.length > 3) 12.sp else 16.sp
                            ),
                            color = colorTag,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val metaText = remember(subject.examDate, subject.startTime, subject.roomNumber, item.completedTopics, item.totalTopics) {
                        buildList {
                            add(DateUtils.formatExamDate(subject.examDate))
                            if (subject.startTime.isNotBlank()) add(subject.startTime)
                            if (subject.roomNumber.isNotBlank()) add("Rm ${subject.roomNumber}")
                            if (item.totalTopics > 0) add("${item.completedTopics}/${item.totalTopics}")
                        }.joinToString(" • ")
                    }
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val context = androidx.compose.ui.platform.LocalContext.current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic.performClick()
                            com.example.util.CalendarExportHelper.exportExamToCalendar(
                                context = context,
                                subject = subject,
                                topics = item.topics
                            )
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("export_calendar_${subject.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Add to Google Calendar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performClick()
                            onEdit()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_subject_${subject.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_subject),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performLongPress()
                            onDelete()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_subject_${subject.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_subject),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Single concise Readiness progress row (smooth capsule bar without M3 stop-dot)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (animatedReadiness > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedReadiness.coerceIn(0.03f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(urgencyColor)
                        )
                    }
                }

                Text(
                    text = "${effectiveReadiness.score}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = urgencyColor
                )
            }

            if (item.topics.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val previewTopics = if (showTopicsInline) item.topics else item.topics.take(3)
                    previewTopics.forEach { topic ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (topic.isDone) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .hapticClickable(hapticType = HapticFeedbackType.LongPress) { onToggleTopic(topic) }
                                .testTag("dashboard_topic_chip_${topic.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (topic.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (topic.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = topic.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    textDecoration = if (topic.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (topic.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )
                            }
                        }
                    }

                    if (item.topics.size > 3) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .hapticClickable { showTopicsInline = !showTopicsInline }
                        ) {
                            Text(
                                text = if (showTopicsInline) "Less" else "+${item.topics.size - 3}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
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

/**
 * Clean, minimal Empty Exams State with Apple Glassmorphic card.
 */
@Composable
fun EmptyDashboardView(
    isFiltered: Boolean,
    onAddSubjectClick: () -> Unit,
    onScanAdmitCardClick: () -> Unit = {},
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 40.dp)
            .appleGlassCard(shape = RoundedCornerShape(26.dp))
            .padding(horizontal = 24.dp, vertical = 40.dp)
            .testTag("empty_dashboard_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isFiltered) {
                stringResource(R.string.no_matching_subjects)
            } else {
                "No upcoming exams"
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isFiltered) {
            Button(
                onClick = onClearFilters,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("clear_filters_button")
            ) {
                Text(stringResource(R.string.clear_filters))
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onAddSubjectClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Add Exam",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .appleGlassPill(shape = RoundedCornerShape(14.dp))
                        .clickable { onScanAdmitCardClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Scan Admit Card",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
