package com.example.ui.routine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExamTrackApplication
import com.example.data.model.AttendanceSafeMarginInfo
import com.example.data.model.ClassRoutineItem
import com.example.data.model.ExamReadinessResult
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.SubjectWithTopics
import com.example.data.model.calculateSmartReadiness
import com.example.data.model. isDueForReview
import com.example.data.model.isReviewOverdue
import com.example.util.DateUtils
import com.example.util.ScannedRoutineCandidate
import com.example.widget.ExamTrackWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DayOfWeek(val key: String, val shortName: String, val fullName: String, val calDay: Int) {
    SATURDAY("SATURDAY", "Sat", "Saturday", Calendar.SATURDAY),
    SUNDAY("SUNDAY", "Sun", "Sunday", Calendar.SUNDAY),
    MONDAY("MONDAY", "Mon", "Monday", Calendar.MONDAY),
    TUESDAY("TUESDAY", "Tue", "Tuesday", Calendar.TUESDAY),
    WEDNESDAY("WEDNESDAY", "Wed", "Wednesday", Calendar.WEDNESDAY),
    THURSDAY("THURSDAY", "Thu", "Thursday", Calendar.THURSDAY),
    FRIDAY("FRIDAY", "Fri", "Friday", Calendar.FRIDAY);

    companion object {
        fun fromCalendar(calDay: Int): DayOfWeek {
            return entries.firstOrNull { it.calDay == calDay } ?: SATURDAY
        }
        fun fromKey(key: String): DayOfWeek {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SATURDAY
        }
    }
}

data class ClassRoutineUiItem(
    val item: ClassRoutineItem,
    val isLiveNow: Boolean = false,
    val isUpcomingToday: Boolean = false,
    val startMinutes: Int = 0,
    val endMinutes: Int = 0,
    val durationFormatted: String = "",
    val examCountdownBadge: String? = null,
    val safeMarginInfo: AttendanceSafeMarginInfo = item.calculateSafeMargin(80)
)

data class RoutineGap(
    val gapId: String = "",
    val index: Int, // position before which gap appears
    val durationMinutes: Int,
    val timeLabel: String,
    val usableStudyMinutes: Int = 0,
    val bufferMinutes: Int = 10,
    val suggestedPomodoroMinutes: Int = 25,
    val activityTypeLabel: String = "25-Min Focus Session",
    val recommendedSubjectId: String? = null,
    val recommendedSubjectName: String = "General Study",
    val recommendedSubjectColor: String = "#0D9488",
    val recommendedTopicTitle: String? = null,
    val recommendationReason: String = "",
    val canFitStudySession: Boolean = false,
    val isDismissed: Boolean = false
)

data class ClassRoutineUiState(
    val selectedDay: DayOfWeek = DayOfWeek.MONDAY,
    val todayDay: DayOfWeek = DayOfWeek.MONDAY,
    val classesForSelectedDay: List<ClassRoutineUiItem> = emptyList(),
    val gapsForSelectedDay: List<RoutineGap> = emptyList(),
    val allRoutines: List<ClassRoutineItem> = emptyList(),
    val classCountByDay: Map<String, Int> = emptyMap(),
    val currentLiveClass: ClassRoutineItem? = null,
    val nextUpcomingClass: ClassRoutineItem? = null,
    val availableSubjects: List<Subject> = emptyList(),
    val attendanceThresholdPercent: Int = 80,
    val campusGapBufferMinutes: Int = 10,
    val examModeEnabled: Boolean = false
)

class ClassRoutineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ExamTrackApplication).repository
    private val settingsPreferences = (application as ExamTrackApplication).settingsPreferences

    private val _uiState = MutableStateFlow(
        ClassRoutineUiState(
            selectedDay = DayOfWeek.fromCalendar(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)),
            todayDay = DayOfWeek.fromCalendar(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        )
    )
    val uiState: StateFlow<ClassRoutineUiState> = _uiState.asStateFlow()

    private var cachedAllRoutines: List<ClassRoutineItem> = emptyList()
    private var cachedSubjectsWithTopics: List<SubjectWithTopics> = emptyList()
    private var cachedStudySessions: List<StudySession> = emptyList()
    private var cachedDismissedGaps: Set<String> = emptySet()
    private var cachedBufferMinutes: Int = 10
    private var cachedThresholdPercent: Int = 80
    private var cachedExamMode: Boolean = false

    init {
        viewModelScope.launch {
            combine(
                repository.allSubjectsWithTopics,
                repository.allStudySessions,
                repository.allClassRoutines
            ) { subjectsWithTopics, sessions, routines ->
                Triple(subjectsWithTopics, sessions, routines)
            }.collect { (subjectsWithTopics, sessions, routines) ->
                cachedSubjectsWithTopics = subjectsWithTopics
                cachedStudySessions = sessions
                cachedAllRoutines = routines
                _uiState.update { it.copy(availableSubjects = subjectsWithTopics.map { s -> s.subject }) }
                refreshRoutinesForSelectedDay()
            }
        }

        viewModelScope.launch {
            combine(
                settingsPreferences.campusGapBufferMinutes,
                settingsPreferences.attendanceThresholdPercent,
                settingsPreferences.dismissedCampusGaps,
                settingsPreferences.examModeEnabled
            ) { buffer, threshold, dismissed, examMode ->
                listOf(buffer, threshold, dismissed, examMode)
            }.collect { values ->
                cachedBufferMinutes = values[0] as Int
                cachedThresholdPercent = values[1] as Int
                @Suppress("UNCHECKED_CAST")
                cachedDismissedGaps = values[2] as Set<String>
                cachedExamMode = values[3] as Boolean
                refreshRoutinesForSelectedDay()
            }
        }
    }

    fun selectDay(day: DayOfWeek) {
        _uiState.update { it.copy(selectedDay = day) }
        refreshRoutinesForSelectedDay()
    }

    fun refreshRoutinesForSelectedDay() {
        val now = System.currentTimeMillis()
        val today = DayOfWeek.fromCalendar(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        val currentMinutes = DateUtils.getCurrentMinutesOfDay()
        val selectedDay = _uiState.value.selectedDay

        val counts = cachedAllRoutines.groupBy { it.dayOfWeek.uppercase() }
            .mapValues { it.value.size }

        val forDay = cachedAllRoutines.filter {
            it.dayOfWeek.equals(selectedDay.key, ignoreCase = true)
        }.sortedBy { DateUtils.parseTimeToMinutes(it.startTime) }

        val isSelectedToday = selectedDay == today

        var liveClass: ClassRoutineItem? = null
        var nextClass: ClassRoutineItem? = null

        val uiItems = forDay.map { routine ->
            val startMin = DateUtils.parseTimeToMinutes(routine.startTime)
            val endMin = DateUtils.parseTimeToMinutes(routine.endTime)
            val isLive = isSelectedToday && DateUtils.isTimeBetween(routine.startTime, routine.endTime, currentMinutes)
            val isUpcoming = isSelectedToday && (startMin > currentMinutes)

            if (isLive && liveClass == null) {
                liveClass = routine
            }
            if (isUpcoming && nextClass == null) {
                nextClass = routine
            }

            val durationMinutes = if (endMin >= startMin && startMin >= 0) endMin - startMin else 0
            val durationFormatted = when {
                durationMinutes <= 0 -> ""
                durationMinutes < 60 -> "${durationMinutes}m"
                durationMinutes % 60 == 0 -> "${durationMinutes / 60}h"
                else -> "${durationMinutes / 60}h ${durationMinutes % 60}m"
            }

            val matchingSubject = _uiState.value.availableSubjects.find {
                it.name.equals(routine.subjectName, ignoreCase = true)
            }
            val todayStart = DateUtils.startOfDay(now)
            val examBadge = if (matchingSubject != null && DateUtils.startOfDay(matchingSubject.examDate) >= todayStart) {
                val countdown = DateUtils.getCountdownInfo(matchingSubject.examDate)
                when {
                    countdown.daysRemaining == 0 -> "Exam Today"
                    countdown.daysRemaining == 1 -> "Exam Tomorrow"
                    else -> "Exam in ${countdown.daysRemaining}d"
                }
            } else null

            ClassRoutineUiItem(
                item = routine,
                isLiveNow = isLive,
                isUpcomingToday = isUpcoming,
                startMinutes = startMin,
                endMinutes = endMin,
                durationFormatted = durationFormatted,
                examCountdownBadge = examBadge,
                safeMarginInfo = routine.calculateSafeMargin(cachedThresholdPercent)
            )
        }

        // Compute prioritized subjects for Campus Gap Focus Mode recommendations
        val prioritizedSubjects: List<Pair<SubjectWithTopics, ExamReadinessResult>> =
            cachedSubjectsWithTopics.map { swt ->
                val focusMins = cachedStudySessions
                    .filter { it.subjectId == swt.subject.id }
                    .sumOf { it.durationMinutes }
                swt to swt.calculateSmartReadiness(actualFocusMinutes = focusMins, nowMillis = now)
            }.filter { (_, readiness) ->
                readiness.daysRemaining >= 0
            }.sortedWith(
                compareBy<Pair<SubjectWithTopics, ExamReadinessResult>> { (_, readiness) ->
                    // Prioritize High Urgency first, then lowest readiness score, then nearest exam
                    when (readiness.urgencyStatus) {
                        com.example.data.model.ExamUrgencyStatus.HIGH_URGENCY -> 0
                        com.example.data.model.ExamUrgencyStatus.NEEDS_ATTENTION -> 1
                        com.example.data.model.ExamUrgencyStatus.ON_TRACK -> 2
                        com.example.data.model.ExamUrgencyStatus.READY -> 3
                        com.example.data.model.ExamUrgencyStatus.COMPLETED -> 4
                    }
                }.thenBy { it.second.daysRemaining }
                    .thenBy { it.second.score }
            )

        val gaps = mutableListOf<RoutineGap>()
        for (i in 0 until uiItems.size - 1) {
            val curr = uiItems[i]
            val next = uiItems[i + 1]
            if (curr.endMinutes > 0 && next.startMinutes > 0 && next.startMinutes > curr.endMinutes) {
                val gapMinutes = next.startMinutes - curr.endMinutes
                if (gapMinutes >= 15) {
                    val gapLabel = when {
                        gapMinutes < 60 -> "${gapMinutes}m Break"
                        gapMinutes % 60 == 0 -> "${gapMinutes / 60}h Break"
                        else -> "${gapMinutes / 60}h ${gapMinutes % 60}m Break"
                    }
                    val usableMins = maxOf(0, gapMinutes - cachedBufferMinutes)
                    val canFit = usableMins >= 15
                    val suggestedPomoMins = when {
                        usableMins >= 50 -> 45
                        usableMins >= 35 -> 30
                        usableMins >= 25 -> 25
                        usableMins >= 20 -> 20
                        usableMins >= 15 -> 15
                        else -> 0
                    }

                    // Pick a subject for this gap (rotate across top urgent subjects if multiple gaps exist)
                    val targetPair = if (prioritizedSubjects.isNotEmpty()) {
                        prioritizedSubjects[gaps.size % prioritizedSubjects.size]
                    } else null

                    val targetSwt = targetPair?.first
                    val targetReadiness = targetPair?.second

                    // Find an overdue review topic or next incomplete topic
                    val overdueTopic = targetSwt?.topics?.firstOrNull { it.isReviewOverdue(now) || it.isDueForReview(now) }
                    val incompleteTopic = targetSwt?.topics?.firstOrNull { !it.isCompleted }
                    val topicSuggestion = overdueTopic?.name ?: incompleteTopic?.name

                    val activityType = when {
                        !canFit -> "Short Transition Break"
                        overdueTopic != null && suggestedPomoMins <= 20 -> "$suggestedPomoMins-Min Quick Revision"
                        suggestedPomoMins >= 45 -> "45-Min Deep Study Block"
                        suggestedPomoMins >= 25 -> "$suggestedPomoMins-Min Focus Session"
                        else -> "$suggestedPomoMins-Min Quick Review"
                    }

                    val reason = when {
                        !canFit -> "Only ${usableMins}m after ${cachedBufferMinutes}m walking buffer — grab water & head to ${next.item.roomNumber.ifBlank { "next class" }}."
                        targetSwt != null && targetReadiness != null -> {
                            val daysText = when (targetReadiness.daysRemaining) {
                                0 -> "Exam TODAY"
                                1 -> "Exam tomorrow"
                                else -> "Exam in ${targetReadiness.daysRemaining} days"
                            }
                            val topicHint = when {
                                overdueTopic != null -> " • Revise overdue topic: \"${overdueTopic.name}\""
                                incompleteTopic != null -> " • Cover topic: \"${incompleteTopic.name}\""
                                else -> ""
                            }
                            "$daysText (Readiness ${targetReadiness.score}%)$topicHint"
                        }
                        else -> "Usable ${usableMins}m window after ${cachedBufferMinutes}m buffer — review notes for ${next.item.subjectName}"
                    }

                    val fallbackSubjName = targetSwt?.subject?.name ?: next.item.subjectName
                    val fallbackSubjColor = targetSwt?.subject?.colorTag ?: next.item.colorTag
                    val gapId = "${selectedDay.key}_${curr.item.endTime}_${next.item.startTime}"

                    gaps.add(
                        RoutineGap(
                            gapId = gapId,
                            index = i + 1,
                            durationMinutes = gapMinutes,
                            timeLabel = "$gapLabel (${curr.item.endTime} – ${next.item.startTime})",
                            usableStudyMinutes = usableMins,
                            bufferMinutes = cachedBufferMinutes,
                            suggestedPomodoroMinutes = suggestedPomoMins,
                            activityTypeLabel = activityType,
                            recommendedSubjectId = targetSwt?.subject?.id,
                            recommendedSubjectName = fallbackSubjName,
                            recommendedSubjectColor = fallbackSubjColor,
                            recommendedTopicTitle = topicSuggestion,
                            recommendationReason = reason,
                            canFitStudySession = canFit,
                            isDismissed = cachedDismissedGaps.contains(gapId)
                        )
                    )
                }
            }
        }

        _uiState.update {
            it.copy(
                todayDay = today,
                classesForSelectedDay = uiItems,
                gapsForSelectedDay = gaps,
                allRoutines = cachedAllRoutines,
                classCountByDay = counts,
                currentLiveClass = liveClass,
                nextUpcomingClass = nextClass,
                attendanceThresholdPercent = cachedThresholdPercent,
                campusGapBufferMinutes = cachedBufferMinutes,
                examModeEnabled = cachedExamMode
            )
        }
    }

    fun dismissCampusGap(gapId: String) {
        viewModelScope.launch {
            settingsPreferences.dismissCampusGap(gapId)
        }
    }

    fun restoreDismissedGaps() {
        viewModelScope.launch {
            settingsPreferences.clearDismissedCampusGaps()
        }
    }

    fun importScannedRoutines(candidates: List<ScannedRoutineCandidate>) {
        viewModelScope.launch {
            val selected = candidates.filter { it.isSelected && it.isValid }
            for (candidate in selected) {
                repository.insertClassRoutine(candidate.toClassRoutineItem())
            }
            ExamTrackWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun addOrUpdateRoutine(
        id: String?,
        subjectName: String,
        teacherName: String,
        roomNumber: String,
        dayOfWeek: DayOfWeek,
        startTime: String,
        endTime: String,
        colorTag: String,
        notes: String
    ) {
        viewModelScope.launch {
            val cleanSubject = subjectName.trim()
            val matchingCourseSlot = cachedAllRoutines.firstOrNull {
                it.id != id && it.subjectName.equals(cleanSubject, ignoreCase = true)
            }
            if (id != null) {
                val existing = cachedAllRoutines.find { it.id == id }
                val updated = ClassRoutineItem(
                    id = id,
                    subjectName = cleanSubject,
                    courseCode = existing?.courseCode ?: matchingCourseSlot?.courseCode ?: "",
                    teacherName = teacherName.trim(),
                    roomNumber = roomNumber.trim(),
                    dayOfWeek = dayOfWeek.key,
                    startTime = startTime.trim(),
                    endTime = endTime.trim(),
                    colorTag = colorTag,
                    notes = notes.trim(),
                    attendedCount = existing?.attendedCount ?: matchingCourseSlot?.attendedCount ?: 0,
                    missedCount = existing?.missedCount ?: matchingCourseSlot?.missedCount ?: 0,
                    lastAttendanceDate = existing?.lastAttendanceDate ?: matchingCourseSlot?.lastAttendanceDate ?: 0L,
                    lastAttendanceStatus = existing?.lastAttendanceStatus ?: matchingCourseSlot?.lastAttendanceStatus ?: 0
                )
                repository.updateClassRoutine(updated)
            } else {
                val newRoutine = ClassRoutineItem(
                    subjectName = cleanSubject,
                    courseCode = matchingCourseSlot?.courseCode ?: "",
                    teacherName = teacherName.trim(),
                    roomNumber = roomNumber.trim(),
                    dayOfWeek = dayOfWeek.key,
                    startTime = startTime.trim(),
                    endTime = endTime.trim(),
                    colorTag = colorTag,
                    notes = notes.trim(),
                    attendedCount = matchingCourseSlot?.attendedCount ?: 0,
                    missedCount = matchingCourseSlot?.missedCount ?: 0,
                    lastAttendanceDate = matchingCourseSlot?.lastAttendanceDate ?: 0L,
                    lastAttendanceStatus = matchingCourseSlot?.lastAttendanceStatus ?: 0
                )
                repository.insertClassRoutine(newRoutine)
            }
            ExamTrackWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteRoutine(id: String) {
        viewModelScope.launch {
            repository.deleteClassRoutineById(id)
            ExamTrackWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    private fun findMatchingCourseSlots(target: ClassRoutineItem): List<ClassRoutineItem> {
        return cachedAllRoutines.filter { slot ->
            slot.id == target.id ||
                (target.courseCode.isNotBlank() && slot.courseCode.equals(target.courseCode, ignoreCase = true)) ||
                slot.subjectName.equals(target.subjectName, ignoreCase = true)
        }
    }

    fun recordAttendance(routineId: String, attended: Boolean) {
        viewModelScope.launch {
            val routine = cachedAllRoutines.find { it.id == routineId } ?: return@launch
            val newAttended = if (attended) routine.attendedCount + 1 else routine.attendedCount
            val newMissed = if (attended) routine.missedCount else routine.missedCount + 1
            val now = System.currentTimeMillis()
            val status = if (attended) 1 else 2

            val matchingSlots = findMatchingCourseSlots(routine).map { slot ->
                slot.copy(
                    attendedCount = newAttended,
                    missedCount = newMissed,
                    lastAttendanceDate = now,
                    lastAttendanceStatus = status
                )
            }
            repository.updateClassRoutines(matchingSlots)
            ExamTrackWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun setAttendanceCounts(routineId: String, attended: Int, missed: Int) {
        viewModelScope.launch {
            val routine = cachedAllRoutines.find { it.id == routineId } ?: return@launch
            val safeAttended = maxOf(0, attended)
            val safeMissed = maxOf(0, missed)
            val now = System.currentTimeMillis()

            val matchingSlots = findMatchingCourseSlots(routine).map { slot ->
                slot.copy(
                    attendedCount = safeAttended,
                    missedCount = safeMissed,
                    lastAttendanceDate = now
                )
            }
            repository.updateClassRoutines(matchingSlots)
            ExamTrackWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun resetAttendance(routineId: String) {
        viewModelScope.launch {
            val routine = cachedAllRoutines.find { it.id == routineId } ?: return@launch
            val matchingSlots = findMatchingCourseSlots(routine).map { slot ->
                slot.copy(
                    attendedCount = 0,
                    missedCount = 0,
                    lastAttendanceDate = 0L,
                    lastAttendanceStatus = 0
                )
            }
            repository.updateClassRoutines(matchingSlots)
            ExamTrackWidgetProvider.updateAllWidgets(getApplication())
        }
    }
}
