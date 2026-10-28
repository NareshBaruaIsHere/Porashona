package com.example.ui.routine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.AttendanceSafeMarginStatus
import com.example.data.model.ClassRoutineItem
import com.example.ui.components.TimetableOcrScannerDialog
import com.example.ui.theme.PresetColorHexes
import com.example.ui.theme.appleGlassCard
import com.example.ui.theme.hapticClickable
import com.example.ui.theme.performClick
import com.example.ui.theme.performLongPress
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassRoutineScreen(
    viewModel: ClassRoutineViewModel = viewModel(),
    onNavigateToPomodoro: (String?) -> Unit = {},
    onStartGapFocusSession: (durationMinutes: Int, subjectId: String?, subjectName: String, subjectColor: String) -> Unit = { _, _, name, _ ->
        onNavigateToPomodoro(name)
    },
    initialWeekGridMode: Boolean = false,
    externalAddClassTrigger: Int = 0,
    externalOcrTrigger: Int = 0,
    externalExportTrigger: Int = 0,
    onExternalAddConsumed: () -> Unit = {},
    onExternalOcrConsumed: () -> Unit = {},
    onExternalExportConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<ClassRoutineItem?>(null) }
    var routineToDelete by remember { mutableStateOf<ClassRoutineItem?>(null) }
    var routineForAttendanceEdit by remember { mutableStateOf<ClassRoutineItem?>(null) }
    var showOcrScannerDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isWeekGridView by remember(initialWeekGridMode) { mutableStateOf(initialWeekGridMode) }

    // Respond to one-shot top-bar or quick-sheet triggers and immediately consume them
    var handledAddTrigger by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(externalAddClassTrigger) {
        if (externalAddClassTrigger > 0 && externalAddClassTrigger != handledAddTrigger) {
            handledAddTrigger = externalAddClassTrigger
            routineToEdit = null
            showAddEditDialog = true
            onExternalAddConsumed()
        } else if (externalAddClassTrigger == 0) {
            handledAddTrigger = 0
        }
    }

    var handledOcrTrigger by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(externalOcrTrigger) {
        if (externalOcrTrigger > 0 && externalOcrTrigger != handledOcrTrigger) {
            handledOcrTrigger = externalOcrTrigger
            showOcrScannerDialog = true
            onExternalOcrConsumed()
        } else if (externalOcrTrigger == 0) {
            handledOcrTrigger = 0
        }
    }

    var handledExportTrigger by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(externalExportTrigger) {
        if (externalExportTrigger > 0 && externalExportTrigger != handledExportTrigger) {
            handledExportTrigger = externalExportTrigger
            showExportDialog = true
            onExternalExportConsumed()
        } else if (externalExportTrigger == 0) {
            handledExportTrigger = 0
        }
    }

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(uiState.selectedDay, isWeekGridView) {
                if (!isWeekGridView) {
                    var totalHorizontalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalHorizontalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            totalHorizontalDrag += dragAmount
                        },
                        onDragEnd = {
                            val swipeThreshold = 72.dp.toPx()
                            val days = DayOfWeek.entries
                            val currentIndex = days.indexOf(uiState.selectedDay)
                            if (totalHorizontalDrag < -swipeThreshold && currentIndex < days.lastIndex) {
                                haptic.performClick()
                                viewModel.selectDay(days[currentIndex + 1])
                            } else if (totalHorizontalDrag > swipeThreshold && currentIndex > 0) {
                                haptic.performClick()
                                viewModel.selectDay(days[currentIndex - 1])
                            }
                        }
                    )
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 112.dp)
        ) {
            // 1. Single Compact Status & Action Bar (Live/Next class status + Day/Week toggle + OCR/Share/Add icons)
            item(key = "compact_status_bar") {
                val featuredClass = uiState.currentLiveClass ?: uiState.nextUpcomingClass ?: uiState.classesForSelectedDay.firstOrNull()?.item
                val statusLabel = when {
                    uiState.currentLiveClass != null -> "LIVE • ${uiState.currentLiveClass!!.subjectName}"
                    uiState.nextUpcomingClass != null && uiState.selectedDay == uiState.todayDay ->
                        "Next • ${uiState.nextUpcomingClass!!.subjectName} (${uiState.nextUpcomingClass!!.startTime})"
                    featuredClass != null ->
                        "${uiState.selectedDay.shortName} • ${uiState.classesForSelectedDay.size} ${if (uiState.classesForSelectedDay.size == 1) "class" else "classes"}"
                    else -> "${uiState.selectedDay.shortName} • Free day"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleGlassCard(shape = RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Compact Status Dot + 1-line summary
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.currentLiveClass != null) {
                                            Color(0xFF10B981)
                                        } else if (featuredClass != null) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right: Compact Day/Week toggle + OCR + Share + Single '+' button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Day | Week Segmented Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .hapticClickable { isWeekGridView = !isWeekGridView }
                                    .testTag("toggle_week_grid_button")
                            ) {
                                Text(
                                    text = if (isWeekGridView) "Week" else "Day",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            // Compact OCR Icon Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .hapticClickable { showOcrScannerDialog = true }
                                    .testTag("scan_routine_ocr_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = "Scan Routine OCR",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Compact Share Icon Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .hapticClickable { showExportDialog = true }
                                    .testTag("export_timetable_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.IosShare,
                                        contentDescription = "Export Schedule",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Single Primary '+' Add Class Button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .hapticClickable(hapticType = HapticFeedbackType.LongPress) {
                                        routineToEdit = null
                                        showAddEditDialog = true
                                    }
                                    .testTag("add_routine_fab")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.add_class_routine),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Clean 7-Day Selector Strip (only in Day view)
            if (!isWeekGridView) {
                item(key = "day_selector_strip") {
                    DaySelectorRow(
                        selectedDay = uiState.selectedDay,
                        todayDay = uiState.todayDay,
                        countsByDay = uiState.classCountByDay,
                        onDaySelected = {
                            viewModel.selectDay(it)
                            isWeekGridView = false
                        }
                    )
                }
            }

            // 5. Either Visual Weekly Time-Grid (Screenshot 4) OR Day List / Calm Empty State (Screenshot 5)
            if (isWeekGridView) {
                item(key = "weekly_time_grid") {
                    WeeklyTimeGridView(
                        allRoutines = uiState.allRoutines,
                        selectedDay = uiState.selectedDay,
                        todayDay = uiState.todayDay,
                        onDayClick = { clickedDay ->
                            viewModel.selectDay(clickedDay)
                            isWeekGridView = false
                        },
                        onRoutineClick = { clicked ->
                            routineToEdit = clicked
                            showAddEditDialog = true
                        }
                    )
                }
            } else if (uiState.classesForSelectedDay.isEmpty()) {
                item(key = "empty_day_state") {
                    EmptyRoutineState(
                        day = uiState.selectedDay,
                        isToday = uiState.selectedDay == uiState.todayDay,
                        onAddClassClick = {
                            routineToEdit = null
                            showAddEditDialog = true
                        },
                        onScanOcrClick = {
                            showOcrScannerDialog = true
                        }
                    )
                }
            } else {
                uiState.classesForSelectedDay.forEachIndexed { index, uiItem ->
                    val gap = uiState.gapsForSelectedDay.firstOrNull { it.index == index }
                    if (gap != null) {
                        item(key = "gap_${gap.gapId.ifBlank { gap.timeLabel }}", contentType = "gap_card") {
                            CampusGapFocusCard(
                                gap = gap,
                                onStartGapSession = { mins, subjId, subjName, subjColor ->
                                    onStartGapFocusSession(mins, subjId, subjName, subjColor)
                                },
                                onDismissGap = { gapId ->
                                    viewModel.dismissCampusGap(gapId)
                                }
                            )
                        }
                    }

                    item(key = uiItem.item.id, contentType = "routine_card") {
                        ClassRoutineCard(
                            uiItem = uiItem,
                            onEditClick = {
                                routineToEdit = uiItem.item
                                showAddEditDialog = true
                            },
                            onDeleteClick = {
                                routineToDelete = uiItem.item
                            },
                            onFocusClick = {
                                onNavigateToPomodoro(uiItem.item.subjectName)
                            },
                            onRecordAttendance = { id, attended ->
                                viewModel.recordAttendance(id, attended)
                            },
                            onEditAttendanceCounts = {
                                routineForAttendanceEdit = uiItem.item
                            }
                        )
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditRoutineDialog(
                routine = routineToEdit,
                defaultDay = uiState.selectedDay,
                availableSubjects = uiState.availableSubjects.map { it.name },
                onDismiss = {
                    showAddEditDialog = false
                    routineToEdit = null
                },
                onSave = { id, subject, teacher, room, day, start, end, color, notes ->
                    viewModel.addOrUpdateRoutine(
                        id = id,
                        subjectName = subject,
                        teacherName = teacher,
                        roomNumber = room,
                        dayOfWeek = day,
                        startTime = start,
                        endTime = end,
                        colorTag = color,
                        notes = notes
                    )
                    showAddEditDialog = false
                    routineToEdit = null
                }
            )
        }

        // Delete Dialog
        if (routineToDelete != null) {
            val toDelete = routineToDelete!!
            AlertDialog(
                onDismissRequest = { routineToDelete = null },
                title = { Text(stringResource(R.string.delete_routine_confirm_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.delete_routine_confirm_message
                        ) + "\n\n${toDelete.subjectName} (${toDelete.startTime} – ${toDelete.endTime})"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteRoutine(toDelete.id)
                            routineToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("confirm_delete_routine")
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { routineToDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Attendance Calculator Edit Dialog
        routineForAttendanceEdit?.let { targetRoutine ->
            EditAttendanceDialog(
                routine = targetRoutine,
                thresholdPercent = uiState.attendanceThresholdPercent,
                onDismiss = { routineForAttendanceEdit = null },
                onSave = { attended, missed ->
                    viewModel.setAttendanceCounts(targetRoutine.id, attended, missed)
                    routineForAttendanceEdit = null
                }
            )
        }

        // On-Device OCR Timetable Scanner Dialog
        if (showOcrScannerDialog) {
            TimetableOcrScannerDialog(
                existingRoutines = uiState.allRoutines,
                defaultDay = uiState.selectedDay,
                onDismiss = { showOcrScannerDialog = false },
                onConfirmImport = { candidates ->
                    viewModel.importScannedRoutines(candidates)
                    showOcrScannerDialog = false
                }
            )
        }

        // One-Tap Timetable Export Dialog
        if (showExportDialog) {
            TimetableExportDialog(
                routines = uiState.allRoutines,
                onDismiss = { showExportDialog = false }
            )
        }
    }
}


/**
 * ClassCue-style Vertical 7-Day Pill Strip (Screenshot 5).
 */
@Composable
fun DaySelectorRow(
    selectedDay: DayOfWeek,
    todayDay: DayOfWeek,
    countsByDay: Map<String, Int>,
    onDaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    // Compute date numbers for the current Saturday-to-Friday week
    val dayNumbers = remember {
        val cal = Calendar.getInstance()
        val todayEntry = DayOfWeek.fromCalendar(cal.get(Calendar.DAY_OF_WEEK))
        DayOfWeek.entries.associateWith { entry ->
            val clone = cal.clone() as Calendar
            clone.add(Calendar.DAY_OF_YEAR, entry.ordinal - todayEntry.ordinal)
            clone.get(Calendar.DAY_OF_MONTH)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = day == selectedDay
            val isToday = day == todayDay
            val count = countsByDay[day.key] ?: 0
            val dayNum = dayNumbers[day] ?: 1

            val containerColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "day_pill_bg"
            )

            val contentColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                label = "day_pill_fg"
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = containerColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .hapticClickable { onDaySelected(day) }
                    .testTag("day_chip_${day.key}")
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = day.shortName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp
                        ),
                        color = if (isSelected) contentColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = dayNum.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = contentColor
                    )

                    // Small dot indicator when classes exist on this day
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    count > 0 && isSelected -> contentColor
                                    count > 0 -> Color(0xFFEF6C57) // Warm coral dot like Screenshot 5
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
            }
        }
    }
}

/**
 * ClassCue-style Visual Weekly Time-Grid View (Screenshot 4).
 */
@Composable
fun WeeklyTimeGridView(
    allRoutines: List<ClassRoutineItem>,
    onRoutineClick: (ClassRoutineItem) -> Unit,
    modifier: Modifier = Modifier,
    selectedDay: DayOfWeek = DayOfWeek.SATURDAY,
    todayDay: DayOfWeek = DayOfWeek.SATURDAY,
    onDayClick: (DayOfWeek) -> Unit = {}
) {
    val gridDays = DayOfWeek.entries
    val (startHour, endHour) = remember(allRoutines) {
        if (allRoutines.isEmpty()) {
            8 to 17
        } else {
            val minStartHour = allRoutines.minOfOrNull {
                (parseMinutesHelper(it.startTime) / 60).coerceIn(6, 20)
            } ?: 8
            val maxEndHour = allRoutines.maxOfOrNull {
                ((parseMinutesHelper(it.endTime) + 59) / 60).coerceIn(9, 22)
            } ?: 17
            minOf(8, minStartHour) to maxOf(17, maxEndHour)
        }
    }
    val hourRowHeight = 64.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .appleGlassCard(shape = RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp)
        ) {
            // 7-Day Header Row (Sat -> Fri)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 42.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                gridDays.forEach { day ->
                    val count = allRoutines.count { it.dayOfWeek.equals(day.key, ignoreCase = true) }
                    val isToday = day == todayDay
                    val isSelected = day == selectedDay
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onDayClick(day) }
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    isToday -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    else -> Color.Transparent
                                }
                            )
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.shortName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isToday || isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = if (count > 0) "$count" else "–",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (count > 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            }
                        )
                    }
                }
            }

            // Time Grid Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourRowHeight * (endHour - startHour + 1))
            ) {
                // Background Hour Lines & Labels
                Column(modifier = Modifier.fillMaxSize()) {
                    for (hour in startHour..endHour) {
                        val hourLabel = when {
                            hour == 0 -> "12 AM"
                            hour == 12 -> "12 PM"
                            hour > 12 -> "${hour - 12} PM"
                            else -> "$hour AM"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(hourRowHeight),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = hourLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(42.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 6.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                            )
                        }
                    }
                }

                // Day Columns with Positioned Course Blocks
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 42.dp)
                ) {
                    gridDays.forEach { day ->
                        val dayClasses = allRoutines.filter {
                            it.dayOfWeek.equals(day.key, ignoreCase = true)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(horizontal = 1.5.dp)
                        ) {
                            dayClasses.forEach { item ->
                                val startMins = parseMinutesHelper(item.startTime)
                                val endMins = parseMinutesHelper(item.endTime)
                                val clampedStart = startMins.coerceIn(startHour * 60, endHour * 60)
                                val durationMins = (endMins - startMins).coerceIn(45, 240)
                                val topOffsetDp = 6.dp + hourRowHeight * ((clampedStart - startHour * 60) / 60f)
                                val blockHeightDp = (hourRowHeight * (durationMins / 60f)).coerceAtMost(
                                    hourRowHeight * (endHour - startHour + 1) - topOffsetDp
                                )

                                val blockColor = try {
                                    Color(android.graphics.Color.parseColor(item.colorTag))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = blockColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = topOffsetDp)
                                        .height(blockHeightDp.coerceAtLeast(36.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onRoutineClick(item) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = item.subjectName,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                lineHeight = 12.sp
                                            ),
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.roomNumber.isNotBlank()) {
                                            Text(
                                                text = item.roomNumber,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                color = Color.White.copy(alpha = 0.88f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

private fun parseMinutesHelper(timeStr: String): Int {
    return try {
        val cleaned = timeStr.trim().uppercase()
        val isPm = cleaned.contains("PM")
        val isAm = cleaned.contains("AM")
        val parts = cleaned.replace("AM", "").replace("PM", "").trim().split(":", ".")
        var hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 9
        val min = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0
        hour * 60 + min
    } catch (_: Exception) {
        9 * 60
    }
}

/**
 * Sleek, uncongested Class Routine Card inspired by Screenshot 2 & 5.
 */
@Composable
fun ClassRoutineCard(
    uiItem: ClassRoutineUiItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onFocusClick: () -> Unit,
    onRecordAttendance: (routineId: String, attended: Boolean) -> Unit,
    onEditAttendanceCounts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val routine = uiItem.item
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val colorTag = remember(routine.colorTag, defaultPrimary) {
        try {
            Color(android.graphics.Color.parseColor(routine.colorTag))
        } catch (e: Exception) {
            defaultPrimary
        }
    }
    var showMenu by remember { mutableStateOf(false) }

    val initials = remember(routine.subjectName) {
        routine.subjectName
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "CL" }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .appleGlassCard(
                shape = RoundedCornerShape(22.dp),
                accentTint = if (uiItem.isLiveNow) MaterialTheme.colorScheme.primary else colorTag
            )
            .testTag("routine_card_${routine.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primary Row: Squircle Avatar + Title & Metadata + More Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar Squircle (Screenshot 2 style)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colorTag.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, colorTag.copy(alpha = 0.45f)),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorTag
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = routine.subjectName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (uiItem.isLiveNow) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981)
                            ) {
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    val detailsLine = remember(routine.startTime, routine.endTime, routine.roomNumber, routine.teacherName) {
                        buildList {
                            add("${routine.startTime} – ${routine.endTime}")
                            if (routine.roomNumber.isNotBlank()) add(routine.roomNumber)
                            if (routine.teacherName.isNotBlank()) add(routine.teacherName)
                        }.joinToString(" • ")
                    }

                    Text(
                        text = detailsLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Focus + Edit/Delete Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onFocusClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Quick Focus Session",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Class") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                },
                                modifier = Modifier.testTag("edit_routine_${routine.id}")
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Attendance Counts") },
                                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditAttendanceCounts()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Class", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                modifier = Modifier.testTag("delete_routine_${routine.id}")
                            )
                        }
                    }
                }
            }

            // Compact Single-Line Attendance Safe-Margin Pill + Quick Present / Absent Taps
            val safeMargin = uiItem.safeMarginInfo
            val statusColor = when (safeMargin.status) {
                AttendanceSafeMarginStatus.SAFE_SKIPPABLE -> Color(0xFF10B981)
                AttendanceSafeMarginStatus.ON_BOUNDARY -> Color(0xFFF59E0B)
                AttendanceSafeMarginStatus.RECOVERY_NEEDED -> Color(0xFFEF4444)
                AttendanceSafeMarginStatus.NO_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val marginHint = when (safeMargin.status) {
                AttendanceSafeMarginStatus.SAFE_SKIPPABLE -> "Safe (+${safeMargin.skippableClasses})"
                AttendanceSafeMarginStatus.ON_BOUNDARY -> "On ${safeMargin.targetPercentage}%"
                AttendanceSafeMarginStatus.RECOVERY_NEEDED -> "Need ${safeMargin.classesNeededToRecover}"
                AttendanceSafeMarginStatus.NO_DATA -> "${safeMargin.targetPercentage}% min"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .clickable { onEditAttendanceCounts() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("attendance_row_${routine.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = if (safeMargin.totalClasses > 0) {
                            "${safeMargin.currentPercentage}% (${safeMargin.attendedClasses}/${safeMargin.totalClasses}) • $marginHint"
                        } else {
                            "Attendance • $marginHint"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (safeMargin.status == AttendanceSafeMarginStatus.RECOVERY_NEEDED) {
                            statusColor
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.18f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .hapticClickable(hapticType = HapticFeedbackType.LongPress) {
                                onRecordAttendance(routine.id, true)
                            }
                            .testTag("attend_plus_${routine.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Present",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Present",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.18f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .hapticClickable(hapticType = HapticFeedbackType.LongPress) {
                                onRecordAttendance(routine.id, false)
                            }
                            .testTag("attend_minus_${routine.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Absent",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Absent",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ClassCue-style Calm Empty State Card (Screenshot 5).
 */
@Composable
fun EmptyRoutineState(
    day: DayOfWeek,
    isToday: Boolean = false,
    onAddClassClick: () -> Unit,
    onScanOcrClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Squircle icon container matching Screenshot 5
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isToday) "No classes today" else "No classes on ${day.shortName}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Full-width dark pill button "+ Add a class on Thu"
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onAddClassClick() }
                    .testTag("empty_add_class_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add a class on ${day.shortName}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRoutineDialog(
    routine: ClassRoutineItem?,
    defaultDay: DayOfWeek,
    availableSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        subjectName: String,
        teacherName: String,
        roomNumber: String,
        dayOfWeek: DayOfWeek,
        startTime: String,
        endTime: String,
        colorTag: String,
        notes: String
    ) -> Unit
) {
    var subjectName by remember { mutableStateOf(routine?.subjectName ?: "") }
    var teacherName by remember { mutableStateOf(routine?.teacherName ?: "") }
    var roomNumber by remember { mutableStateOf(routine?.roomNumber ?: "") }
    var selectedDay by remember {
        mutableStateOf(
            if (routine != null) DayOfWeek.fromKey(routine.dayOfWeek) else defaultDay
        )
    }
    var startTime by remember {
        mutableStateOf(DateUtils.sanitize12HourTime(routine?.startTime ?: "09:00 AM", 9, 0))
    }
    var endTime by remember {
        mutableStateOf(DateUtils.sanitize12HourTime(routine?.endTime ?: "10:15 AM", 10, 15))
    }
    var selectedColor by remember { mutableStateOf(routine?.colorTag ?: PresetColorHexes.first()) }
    var notes by remember { mutableStateOf(routine?.notes ?: "") }
    var showError by remember { mutableStateOf(false) }

    // null = closed, true = editing startTime, false = editing endTime
    var editingStartTimeClock by remember { mutableStateOf<Boolean?>(null) }

    val timeFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = Color.Transparent
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (routine == null) stringResource(R.string.add_class_routine) else stringResource(R.string.edit_class_routine),
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
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = {
                        subjectName = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.subject_name), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("e.g. Data Structures", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    isError = showError && subjectName.isBlank(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_subject_input")
                )

                if (availableSubjects.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableSubjects.take(6).forEach { existingSub ->
                            FilterChip(
                                selected = subjectName.equals(existingSub, ignoreCase = true),
                                onClick = { subjectName = existingSub },
                                label = { Text(existingSub, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.day_of_week),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(day.shortName) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = {
                            Text(
                                text = stringResource(R.string.start_time),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Select Start Time",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        colors = timeFieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { editingStartTimeClock = true }
                            .testTag("routine_start_input")
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = {
                            Text(
                                text = stringResource(R.string.end_time),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Select End Time",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        colors = timeFieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { editingStartTimeClock = false }
                            .testTag("routine_end_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = {
                            Text(
                                text = stringResource(R.string.room_number),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.room_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("routine_room_input")
                    )
                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { teacherName = it },
                        label = {
                            Text(
                                text = stringResource(R.string.professor_sir_name),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.professor_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("routine_teacher_input")
                    )
                }

                Text(
                    text = stringResource(R.string.color_tag),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PresetColorHexes.forEach { hex ->
                        val c = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .hapticClickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_optional), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subjectName.isBlank()) {
                        showError = true
                    } else {
                        onSave(
                            routine?.id,
                            subjectName,
                            teacherName,
                            roomNumber,
                            selectedDay,
                            startTime,
                            endTime,
                            selectedColor,
                            notes
                        )
                    }
                },
                modifier = Modifier.testTag("save_routine_button")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    // Material 3 Clock TimePicker Dialog (with AM / PM selector)
    editingStartTimeClock?.let { isEditingStart ->
        val currentTimeString = if (isEditingStart) startTime else endTime
        val (initialHour, initialMinute) = remember(currentTimeString) {
            DateUtils.parse24HourAndMinute(
                timeStr = currentTimeString,
                defaultHour = if (isEditingStart) 9 else 10,
                defaultMinute = if (isEditingStart) 0 else 15
            )
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { editingStartTimeClock = null },
            title = {
                Text(
                    text = if (isEditingStart) stringResource(R.string.start_time) else stringResource(R.string.end_time),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.testTag("routine_clock_time_picker")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formatted = DateUtils.format12HourTime(
                            hour24 = timePickerState.hour,
                            minute = timePickerState.minute
                        )
                        if (isEditingStart) {
                            startTime = formatted
                        } else {
                            endTime = formatted
                        }
                        editingStartTimeClock = null
                    },
                    modifier = Modifier.testTag("confirm_clock_time_button")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingStartTimeClock = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
