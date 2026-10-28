package com.example.ui.detail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExamTrackApplication
import com.example.data.model.EbbinghausIntervals
import com.example.data.model.ExamReadinessResult
import com.example.data.model.SubjectWithTopics
import com.example.data.model.Topic
import com.example.data.model.calculateSmartReadiness
import com.example.notification.NotificationHelper
import com.example.util.OcrScannerHelper
import com.example.widget.ExamTrackWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SubjectDetailUiState(
    val subjectWithTopics: SubjectWithTopics? = null,
    val readiness: ExamReadinessResult? = null,
    val loggedFocusMinutes: Int = 0,
    val examModeEnabled: Boolean = false,
    val readinessAlertsEnabled: Boolean = true,
    val newTopicTitle: String = "",
    val showDeleteDialog: Boolean = false,
    val showVaultEditDialog: Boolean = false,
    val isSubjectDeleted: Boolean = false,
    val isLoading: Boolean = true
)

class SubjectDetailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = (application as ExamTrackApplication).repository
    private val settingsPreferences = (application as ExamTrackApplication).settingsPreferences
    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    private var currentSubjectId: String = ""

    fun initialize(subjectId: String) {
        currentSubjectId = subjectId
        viewModelScope.launch {
            combine(
                repository.getSubjectWithTopics(subjectId),
                repository.getStudySessionsForSubject(subjectId),
                settingsPreferences.examModeEnabled,
                settingsPreferences.readinessAlertsEnabled
            ) { data, sessions, examMode, readinessAlerts ->
                val focusMins = sessions.sumOf { it.durationMinutes }
                val readinessResult = data?.calculateSmartReadiness(
                    actualFocusMinutes = focusMins,
                    nowMillis = System.currentTimeMillis()
                )
                SubjectDetailSnapshot(data, readinessResult, focusMins, examMode, readinessAlerts)
            }.collect { snapshot ->
                _uiState.update { current ->
                    current.copy(
                        subjectWithTopics = snapshot.data,
                        readiness = snapshot.readiness,
                        loggedFocusMinutes = snapshot.focusMinutes,
                        examModeEnabled = snapshot.examMode,
                        readinessAlertsEnabled = snapshot.readinessAlerts,
                        isLoading = false,
                        isSubjectDeleted = current.isSubjectDeleted || (snapshot.data == null && !current.isLoading)
                    )
                }
            }
        }
    }

    private data class SubjectDetailSnapshot(
        val data: SubjectWithTopics?,
        val readiness: ExamReadinessResult?,
        val focusMinutes: Int,
        val examMode: Boolean,
        val readinessAlerts: Boolean
    )

    fun onNewTopicTitleChange(title: String) {
        _uiState.update { it.copy(newTopicTitle = title) }
    }

    fun addTopic() {
        val title = _uiState.value.newTopicTitle.trim()
        if (title.isEmpty() || currentSubjectId.isEmpty()) return

        val currentTopics = _uiState.value.subjectWithTopics?.topics ?: emptyList()
        val newOrder = currentTopics.size

        viewModelScope.launch {
            val newTopic = Topic(
                subjectId = currentSubjectId,
                title = title,
                isDone = false,
                order = newOrder
            )
            repository.insertTopic(newTopic)
            _uiState.update { it.copy(newTopicTitle = "") }
            ExamTrackWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleTopicDone(topic: Topic) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val context = getApplication<Application>()
            val subjectName = _uiState.value.subjectWithTopics?.subject?.name ?: "Study Subject"
            val becomingDone = !topic.isDone

            val updated = if (becomingDone) {
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

            if (becomingDone) {
                val notifyEnabled = settingsPreferences.notificationsEnabled.first()
                val revisionEnabled = settingsPreferences.revisionRemindersEnabled.first()
                if (notifyEnabled && revisionEnabled) {
                    NotificationHelper.scheduleRevisionReminder(context, updated, subjectName)
                }
            } else {
                NotificationHelper.cancelRevisionReminder(context, topic.id)
            }

            ExamTrackWidgetProvider.updateAllWidgets(context)
        }
    }

    /**
     * Marks a completed topic as reviewed today and advances it along the Ebbinghaus
     * spaced-repetition curve (1 -> 3 -> 7 -> 14 -> 30 days).
     */
    fun markTopicReviewed(topic: Topic) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val context = getApplication<Application>()
            val subjectName = _uiState.value.subjectWithTopics?.subject?.name ?: "Study Subject"
            val currentStage = maxOf(1, topic.reviewCount + 1)
            val nextReview = EbbinghausIntervals.computeNextReviewTimestamp(now, currentStage)

            val updated = topic.copy(
                isDone = true,
                reviewCount = currentStage,
                lastReviewedAt = now,
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

    /**
     * Snoozes a due or overdue spaced-repetition review by [days] (default 1 day).
     */
    fun snoozeTopicReview(topic: Topic, days: Int = 1) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val context = getApplication<Application>()
            val subjectName = _uiState.value.subjectWithTopics?.subject?.name ?: "Study Subject"
            val snoozedTimestamp = EbbinghausIntervals.snoozeReviewTimestamp(now, days)

            val updated = topic.copy(nextReviewAt = snoozedTimestamp)
            repository.updateTopic(updated)

            val notifyEnabled = settingsPreferences.notificationsEnabled.first()
            val revisionEnabled = settingsPreferences.revisionRemindersEnabled.first()
            if (notifyEnabled && revisionEnabled) {
                NotificationHelper.scheduleRevisionReminder(context, updated, subjectName)
            }
        }
    }

    fun updateExamHallVault(
        roomNumber: String,
        examHallBuilding: String,
        seatNumber: String,
        rollNumber: String,
        expectedStudyHours: Int,
        referenceLinks: String,
        notes: String
    ) {
        val currentSubject = _uiState.value.subjectWithTopics?.subject ?: return
        viewModelScope.launch {
            val updated = currentSubject.copy(
                roomNumber = roomNumber.trim(),
                examHallBuilding = examHallBuilding.trim(),
                seatNumber = seatNumber.trim(),
                rollNumber = rollNumber.trim(),
                expectedStudyHours = expectedStudyHours.coerceIn(1, 200).toFloat(),
                referenceLinks = referenceLinks.trim(),
                formulaNotes = notes.trim()
            )
            repository.updateSubject(updated)
            _uiState.update { it.copy(showVaultEditDialog = false) }
        }
    }

    fun attachFormulaSheet(uri: Uri) {
        val currentSubject = _uiState.value.subjectWithTopics?.subject ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val savedPath = withContext(Dispatchers.IO) {
                OcrScannerHelper.copyAttachmentToVaultStorage(context, uri, currentSubject.id)
            }
            if (savedPath != null) {
                repository.updateSubject(currentSubject.copy(formulaSheetUri = savedPath))
            }
        }
    }

    fun removeFormulaSheet() {
        val currentSubject = _uiState.value.subjectWithTopics?.subject ?: return
        viewModelScope.launch {
            repository.updateSubject(currentSubject.copy(formulaSheetUri = ""))
        }
    }

    fun showVaultEditDialog(show: Boolean) {
        _uiState.update { it.copy(showVaultEditDialog = show) }
    }

    fun deleteTopic(topicId: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            NotificationHelper.cancelRevisionReminder(context, topicId)
            repository.deleteTopicById(topicId)
            ExamTrackWidgetProvider.updateAllWidgets(context)
        }
    }

    fun moveTopicUp(topic: Topic) {
        val topics = _uiState.value.subjectWithTopics?.topics ?: return
        val index = topics.indexOfFirst { it.id == topic.id }
        if (index > 0) {
            val mutableList = topics.toMutableList()
            val temp = mutableList[index]
            mutableList[index] = mutableList[index - 1]
            mutableList[index - 1] = temp
            viewModelScope.launch {
                repository.updateTopicsOrder(mutableList)
            }
        }
    }

    fun moveTopicDown(topic: Topic) {
        val topics = _uiState.value.subjectWithTopics?.topics ?: return
        val index = topics.indexOfFirst { it.id == topic.id }
        if (index in 0 until topics.size - 1) {
            val mutableList = topics.toMutableList()
            val temp = mutableList[index]
            mutableList[index] = mutableList[index + 1]
            mutableList[index + 1] = temp
            viewModelScope.launch {
                repository.updateTopicsOrder(mutableList)
            }
        }
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteDialog = show) }
    }

    fun deleteSubject() {
        if (currentSubjectId.isEmpty()) return
        _uiState.update { it.copy(showDeleteDialog = false, isSubjectDeleted = true) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            NotificationHelper.cancelExamReminders(context, currentSubjectId)
            val topics = _uiState.value.subjectWithTopics?.topics ?: emptyList()
            for (t in topics) {
                NotificationHelper.cancelRevisionReminder(context, t.id)
            }
            repository.deleteSubjectById(currentSubjectId)
            ExamTrackWidgetProvider.updateAllWidgets(context)
        }
    }
}
