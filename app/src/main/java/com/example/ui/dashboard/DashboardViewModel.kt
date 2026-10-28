package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExamTrackApplication
import com.example.data.model.EbbinghausIntervals
import com.example.data.model.ExamReadinessResult
import com.example.data.model.ExamUrgencyStatus
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.SubjectWithTopics
import com.example.data.model.Topic
import com.example.data.model.calculateSmartReadiness
import com.example.data.model.isDueForReview
import com.example.data.model.isReviewOverdue
import com.example.notification.NotificationHelper
import com.example.util.CountdownStatus
import com.example.util.DateUtils
import com.example.util.ScannedExamCandidate
import com.example.widget.ExamTrackWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class DueRevisionItem(
    val subject: Subject,
    val topic: Topic,
    val isOverdue: Boolean
) {
    val subjectId: String get() = subject.id
    val subjectName: String get() = subject.name
}

typealias TopicWithSubjectInfo = DueRevisionItem

data class DashboardUiState(
    val subjects: List<SubjectWithTopics> = emptyList(),
    val subjectStudyHours: Map<String, Float> = emptyMap(),
    val subjectStudyMinutes: Map<String, Int> = emptyMap(),
    val subjectReadinessMap: Map<String, ExamReadinessResult> = emptyMap(),
    val dueRevisionItems: List<DueRevisionItem> = emptyList(),
    val completedFocusMinutesToday: Int = 0,
    val dailyFocusTargetMinutes: Int = 120,
    val examModeEnabled: Boolean = false,
    val readinessAlertsEnabled: Boolean = true,
    val isLoading: Boolean = false
) {
    val readinessBySubject: Map<String, ExamReadinessResult> get() = subjectReadinessMap
    val studyMinutesBySubject: Map<String, Int> get() = subjectStudyMinutes
    val dueRevisionTopics: List<DueRevisionItem> get() = dueRevisionItems

    fun getStudyHours(subjectId: String): Float = subjectStudyHours[subjectId] ?: 0f
    fun getStudyMinutes(subjectId: String): Int = subjectStudyMinutes[subjectId] ?: 0

    fun getReadiness(item: SubjectWithTopics): ExamReadinessResult {
        return subjectReadinessMap[item.subject.id]
            ?: item.calculateSmartReadiness(actualFocusMinutes = getStudyMinutes(item.subject.id))
    }

    val totalSubjects: Int get() = subjects.size
    val totalTopics: Int get() = subjects.sumOf { it.totalTopics }
    val completedTopics: Int get() = subjects.sumOf { it.completedTopics }
    val remainingTopics: Int get() = (totalTopics - completedTopics).coerceAtLeast(0)
    val overallProgress: Float
        get() = if (totalTopics > 0) completedTopics.toFloat() / totalTopics.toFloat() else 0f
    val overallPercentage: Int get() = (overallProgress * 100).toInt()

    val averageReadinessScore: Int
        get() {
            val activeReadiness = subjects.map { getReadiness(it) }
                .filter { it.daysRemaining >= 0 }
            return if (activeReadiness.isNotEmpty()) {
                activeReadiness.map { it.score }.average().toInt()
            } else if (subjects.isNotEmpty()) {
                subjects.map { getReadiness(it).score }.average().toInt()
            } else 0
        }

    val highUrgencyExams: List<Pair<SubjectWithTopics, ExamReadinessResult>>
        get() = subjects.map { it to getReadiness(it) }
            .filter { (_, r) ->
                r.daysRemaining >= 0 &&
                    (r.urgencyStatus == ExamUrgencyStatus.HIGH_URGENCY || r.urgencyStatus == ExamUrgencyStatus.NEEDS_ATTENTION)
            }
            .sortedWith(
                compareBy<Pair<SubjectWithTopics, ExamReadinessResult>> {
                    if (it.second.urgencyStatus == ExamUrgencyStatus.HIGH_URGENCY) 0 else 1
                }.thenBy { it.second.daysRemaining }
                    .thenBy { it.second.score }
            )

    val upcomingCount: Int
        get() = subjects.count {
            val status = DateUtils.getCountdownInfo(it.subject.examDate, startTime = it.subject.startTime).status
            status == CountdownStatus.UPCOMING || status == CountdownStatus.URGENT || status == CountdownStatus.TODAY
        }

    val urgentCount: Int
        get() = subjects.count {
            val status = DateUtils.getCountdownInfo(it.subject.examDate, startTime = it.subject.startTime).status
            val readiness = getReadiness(it)
            status == CountdownStatus.URGENT || status == CountdownStatus.TODAY ||
                readiness.urgencyStatus == ExamUrgencyStatus.HIGH_URGENCY
        }

    val todayCount: Int
        get() = subjects.count {
            DateUtils.getCountdownInfo(it.subject.examDate, startTime = it.subject.startTime).status == CountdownStatus.TODAY
        }

    val passedCount: Int
        get() = subjects.count {
            DateUtils.getCountdownInfo(it.subject.examDate, startTime = it.subject.startTime).status == CountdownStatus.PASSED
        }

    val nearestUpcomingExam: SubjectWithTopics?
        get() {
            val todayStart = DateUtils.startOfDay(System.currentTimeMillis())
            return subjects.firstOrNull { DateUtils.startOfDay(it.subject.examDate) >= todayStart }
                ?: subjects.firstOrNull()
        }
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ExamTrackApplication).repository
    private val settingsPreferences = (application as ExamTrackApplication).settingsPreferences

    private data class PrefsBundle(
        val examMode: Boolean,
        val readinessAlerts: Boolean,
        val dailyTargetMins: Int
    )

    private val prefsFlow = combine(
        settingsPreferences.examModeEnabled,
        settingsPreferences.readinessAlertsEnabled,
        settingsPreferences.dailyFocusTargetMinutes
    ) { examMode, readinessAlerts, dailyTarget ->
        PrefsBundle(examMode, readinessAlerts, dailyTarget)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allSubjectsWithTopics,
        repository.allStudySessions,
        prefsFlow
    ) { list, sessions, prefs ->
        val now = System.currentTimeMillis()
        val todayStart = DateUtils.startOfDay(now)
        val todayEnd = todayStart + TimeUnit.DAYS.toMillis(1)

        val minutesMap = sessions.filter { !it.subjectId.isNullOrBlank() }
            .groupBy { it.subjectId!! }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }

        val hoursMap = minutesMap.mapValues { (_, mins) -> mins / 60.0f }

        val readinessMap = list.associate { swt ->
            val mins = minutesMap[swt.subject.id] ?: 0
            swt.subject.id to swt.calculateSmartReadiness(actualFocusMinutes = mins, nowMillis = now)
        }

        // Sort subjects: upcoming exams first, ordered by date (or by urgency when Exam Mode is enabled)
        val sortedList = if (prefs.examMode) {
            list.sortedWith(
                compareBy<SubjectWithTopics> {
                    val r = readinessMap[it.subject.id]
                    if ((r?.daysRemaining ?: -1) < 0) 1 else 0
                }.thenBy {
                    val r = readinessMap[it.subject.id]
                    when (r?.urgencyStatus) {
                        ExamUrgencyStatus.HIGH_URGENCY -> 0
                        ExamUrgencyStatus.NEEDS_ATTENTION -> 1
                        ExamUrgencyStatus.ON_TRACK -> 2
                        ExamUrgencyStatus.READY -> 3
                        else -> 4
                    }
                }.thenBy { it.subject.examDate }
            )
        } else {
            list.sortedBy { it.subject.examDate }
        }

        val completedTodayMins = sessions
            .filter { it.dateMillis in todayStart until todayEnd }
            .sumOf { it.durationMinutes }

        val dueRevisions = mutableListOf<DueRevisionItem>()
        for (swt in sortedList) {
            for (topic in swt.topics) {
                val overdue = topic.isReviewOverdue(now)
                val dueToday = topic.isDueForReview(now)
                if (overdue || dueToday) {
                    dueRevisions.add(
                        DueRevisionItem(
                            subject = swt.subject,
                            topic = topic,
                            isOverdue = overdue
                        )
                    )
                }
            }
        }
        dueRevisions.sortWith(
            compareBy<DueRevisionItem> { if (it.isOverdue) 0 else 1 }
                .thenBy { if (it.topic.nextReviewAt > 0L) it.topic.nextReviewAt else Long.MAX_VALUE }
        )

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            ExamTrackWidgetProvider.updateAllWidgets(getApplication())
        }

        DashboardUiState(
            subjects = sortedList,
            subjectStudyHours = hoursMap,
            subjectStudyMinutes = minutesMap,
            subjectReadinessMap = readinessMap,
            dueRevisionItems = dueRevisions,
            completedFocusMinutesToday = completedTodayMins,
            dailyFocusTargetMinutes = prefs.dailyTargetMins,
            examModeEnabled = prefs.examMode,
            readinessAlertsEnabled = prefs.readinessAlerts,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun setExamMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setExamModeEnabled(enabled)
        }
    }

    fun toggleExamMode() {
        viewModelScope.launch {
            val current = settingsPreferences.examModeEnabled.first()
            settingsPreferences.setExamModeEnabled(!current)
        }
    }

    fun toggleTopic(topic: Topic) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val context = getApplication<Application>()
            val willBeDone = !topic.isDone
            val subjectName = uiState.value.subjects
                .firstOrNull { it.subject.id == topic.subjectId }
                ?.subject?.name ?: "Study Subject"

            val updated = if (willBeDone) {
                val nextReview = EbbinghausIntervals.computeNextReviewTimestamp(now, 0)
                topic.copy(
                    isDone = true,
                    completedAt = now,
                    reviewCount = maxOf(0, topic.reviewCount),
                    lastReviewedAt = now,
                    nextReviewAt = nextReview
                )
            } else {
                topic.copy(
                    isDone = false,
                    nextReviewAt = 0L
                )
            }
            repository.updateTopic(updated)

            if (willBeDone) {
                val notifyEnabled = settingsPreferences.notificationsEnabled.first()
                val revisionEnabled = settingsPreferences.revisionRemindersEnabled.first()
                if (notifyEnabled && revisionEnabled) {
                    NotificationHelper.scheduleRevisionReminder(context, updated, subjectName)
                }
            } else {
                NotificationHelper.cancelRevisionReminder(context, topic.id)
            }
        }
    }

    fun markTopicReviewed(topic: Topic) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val context = getApplication<Application>()
            val subjectName = uiState.value.subjects
                .firstOrNull { it.subject.id == topic.subjectId }
                ?.subject?.name ?: "Study Subject"
            val currentStage = maxOf(1, topic.reviewCount + 1)
            val nextReview = EbbinghausIntervals.computeNextReviewTimestamp(now, currentStage)

            val updated = topic.copy(
                isDone = true,
                lastReviewedAt = now,
                reviewCount = currentStage,
                nextReviewAt = nextReview
            )
            repository.updateTopic(updated)

            val notifyEnabled = settingsPreferences.notificationsEnabled.first()
            val revisionEnabled = settingsPreferences.revisionRemindersEnabled.first()
            if (notifyEnabled && revisionEnabled) {
                NotificationHelper.scheduleRevisionReminder(context, updated, subjectName)
            }
        }
    }

    fun snoozeTopicReview(topic: Topic, days: Int = 1) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val context = getApplication<Application>()
            val subjectName = uiState.value.subjects
                .firstOrNull { it.subject.id == topic.subjectId }
                ?.subject?.name ?: "Study Subject"
            val snoozed = EbbinghausIntervals.snoozeReviewTimestamp(now, days)
            val updated = topic.copy(nextReviewAt = snoozed)
            repository.updateTopic(updated)

            val notifyEnabled = settingsPreferences.notificationsEnabled.first()
            val revisionEnabled = settingsPreferences.revisionRemindersEnabled.first()
            if (notifyEnabled && revisionEnabled) {
                NotificationHelper.scheduleRevisionReminder(context, updated, subjectName)
            }
        }
    }

    fun addTopic(subjectId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val newTopic = Topic(
                subjectId = subjectId,
                title = title.trim(),
                isDone = false,
                order = 999
            )
            repository.insertTopic(newTopic)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            NotificationHelper.cancelExamReminders(context, subject.id)
            repository.deleteSubject(subject)
            ExamTrackWidgetProvider.updateAllWidgets(context)
        }
    }

    fun importScannedExams(candidates: List<ScannedExamCandidate>) {
        importScannedExamCandidates(candidates)
    }

    fun importScannedExamCandidates(candidates: List<ScannedExamCandidate>) {
        val selected = candidates
            .filter { it.isSelected && it.isValid }
            .map { it.sanitized() }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val existingList = repository.getAllSubjects()
            for (candidate in selected) {
                val match = existingList.firstOrNull {
                    it.name.equals(candidate.subjectName, ignoreCase = true)
                }
                if (match != null) {
                    val updated = match.copy(
                        examDate = candidate.examDateMillis?.let { DateUtils.startOfDay(it) } ?: match.examDate,
                        startTime = candidate.startTime.ifBlank { match.startTime },
                        endTime = candidate.endTime.ifBlank { match.endTime },
                        roomNumber = candidate.roomNumber.ifBlank { match.roomNumber },
                        examHallBuilding = candidate.examHallBuilding.ifBlank { match.examHallBuilding },
                        seatNumber = candidate.seatNumber.ifBlank { match.seatNumber },
                        rollNumber = candidate.rollNumber.ifBlank { match.rollNumber }
                    )
                    repository.updateSubject(updated)
                    NotificationHelper.cancelExamReminders(context, updated.id)
                    NotificationHelper.scheduleExamReminders(context, updated)
                } else {
                    val created = candidate.toSubject()
                    repository.insertSubject(created)
                    NotificationHelper.scheduleExamReminders(context, created)
                }
            }
            ExamTrackWidgetProvider.updateAllWidgets(context)
        }
    }
}
