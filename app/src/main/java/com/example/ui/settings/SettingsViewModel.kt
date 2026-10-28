package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExamTrackApplication
import com.example.data.preferences.ThemeMode
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsPreferences = (application as ExamTrackApplication).settingsPreferences
    private val repository = (application as ExamTrackApplication).repository

    val notificationsEnabled: StateFlow<Boolean> = settingsPreferences.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dailyStudyReminderEnabled: StateFlow<Boolean> = settingsPreferences.dailyStudyReminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dailyStudyReminderHour: StateFlow<Int> = settingsPreferences.dailyStudyReminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    val dailyStudyReminderMinute: StateFlow<Int> = settingsPreferences.dailyStudyReminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val hapticFeedbackEnabled: StateFlow<Boolean> = settingsPreferences.hapticFeedbackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<ThemeMode> = settingsPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val examModeEnabled: StateFlow<Boolean> = settingsPreferences.examModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val revisionRemindersEnabled: StateFlow<Boolean> = settingsPreferences.revisionRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val readinessAlertsEnabled: StateFlow<Boolean> = settingsPreferences.readinessAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dailyFocusTargetMinutes: StateFlow<Int> = settingsPreferences.dailyFocusTargetMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 120)

    val defaultFocusMinutes: StateFlow<Int> = settingsPreferences.defaultFocusMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val defaultBreakMinutes: StateFlow<Int> = settingsPreferences.defaultBreakMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val attendanceThresholdPercent: StateFlow<Int> = settingsPreferences.attendanceThresholdPercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 80)

    val campusGapBufferMinutes: StateFlow<Int> = settingsPreferences.campusGapBufferMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    private val _backupStatusMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage

    fun clearBackupStatusMessage() {
        _backupStatusMessage.value = null
    }

    fun exportBackupToUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = com.example.util.BackupRestoreManager.exportBackupToUri(
                context = context,
                uri = uri,
                repository = repository
            )
            result.onSuccess { summary ->
                _backupStatusMessage.value =
                    "Backup saved (${summary.subjectsCount} exams, ${summary.routinesCount} classes)"
            }.onFailure { err ->
                _backupStatusMessage.value =
                    "Backup failed: ${err.localizedMessage ?: "Unable to write file"}"
            }
        }
    }

    fun importBackupFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = com.example.util.BackupRestoreManager.importBackupFromUri(
                context = context,
                uri = uri,
                repository = repository,
                clearBeforeRestore = false
            )
            result.onSuccess { summary ->
                if (settingsPreferences.notificationsEnabled.first()) {
                    for (subject in repository.getAllSubjects()) {
                        NotificationHelper.scheduleExamReminders(context, subject)
                    }
                    if (settingsPreferences.revisionRemindersEnabled.first()) {
                        rescheduleAllRevisionReminders(context)
                    }
                }
                com.example.widget.ExamTrackWidgetProvider.updateAllWidgets(context)
                _backupStatusMessage.value =
                    "Restored ${summary.subjectsCount} exams & ${summary.routinesCount} classes"
            }.onFailure { err ->
                _backupStatusMessage.value =
                    "Restore failed: ${err.localizedMessage ?: "Invalid backup JSON"}"
            }
        }
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val existingSubjects = repository.getAllSubjects()
            for (subject in existingSubjects) {
                NotificationHelper.cancelExamReminders(context, subject.id)
            }
            cancelAllRevisionReminders(context)
            repository.clearAllData()
            settingsPreferences.clearDismissedCampusGaps()
            settingsPreferences.resetFocusIntegrityStreak()
            com.example.widget.ExamTrackWidgetProvider.updateAllWidgets(context)
            _backupStatusMessage.value = "All local schedule and study data cleared"
        }
    }

    fun toggleExamMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setExamModeEnabled(enabled)
        }
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setHapticFeedbackEnabled(enabled)
        }
    }

    fun toggleDailyStudyReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setDailyStudyReminderEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                val hour = settingsPreferences.dailyStudyReminderHour.first()
                val minute = settingsPreferences.dailyStudyReminderMinute.first()
                NotificationHelper.scheduleDailyStudyReminder(context, hour, minute)
            } else {
                NotificationHelper.cancelDailyStudyReminder(context)
            }
        }
    }

    fun setDailyStudyReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsPreferences.setDailyStudyReminderTime(hour, minute)
            val context = getApplication<Application>()
            if (settingsPreferences.dailyStudyReminderEnabled.first()) {
                NotificationHelper.scheduleDailyStudyReminder(context, hour, minute)
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setNotificationsEnabled(enabled)
            val context = getApplication<Application>()
            val subjects = repository.getAllSubjects()
            if (enabled) {
                for (subject in subjects) {
                    NotificationHelper.scheduleExamReminders(context, subject)
                }
                if (settingsPreferences.revisionRemindersEnabled.first()) {
                    rescheduleAllRevisionReminders(context)
                }
            } else {
                for (subject in subjects) {
                    NotificationHelper.cancelExamReminders(context, subject.id)
                }
                cancelAllRevisionReminders(context)
            }
        }
    }

    fun toggleRevisionReminders(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setRevisionRemindersEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled && settingsPreferences.notificationsEnabled.first()) {
                rescheduleAllRevisionReminders(context)
            } else {
                cancelAllRevisionReminders(context)
            }
        }
    }

    fun toggleReadinessAlerts(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setReadinessAlertsEnabled(enabled)
        }
    }

    fun syncPermissionDenied() {
        viewModelScope.launch {
            settingsPreferences.setAllReminderToggles(false)
            val context = getApplication<Application>()
            NotificationHelper.cancelDailyStudyReminder(context)
            for (subject in repository.getAllSubjects()) {
                NotificationHelper.cancelExamReminders(context, subject.id)
            }
            cancelAllRevisionReminders(context)
        }
    }

    fun onPermissionGrantedForDefaultReminders() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (settingsPreferences.dailyStudyReminderEnabled.first()) {
                val hour = settingsPreferences.dailyStudyReminderHour.first()
                val minute = settingsPreferences.dailyStudyReminderMinute.first()
                NotificationHelper.scheduleDailyStudyReminder(context, hour, minute)
            }
            if (settingsPreferences.notificationsEnabled.first()) {
                for (subject in repository.getAllSubjects()) {
                    NotificationHelper.scheduleExamReminders(context, subject)
                }
                if (settingsPreferences.revisionRemindersEnabled.first()) {
                    rescheduleAllRevisionReminders(context)
                }
            }
        }
    }

    fun setDailyFocusTargetMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsPreferences.setDailyFocusTargetMinutes(minutes)
        }
    }

    fun setDefaultFocusMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsPreferences.setDefaultFocusMinutes(minutes)
        }
    }

    fun setDefaultBreakMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsPreferences.setDefaultBreakMinutes(minutes)
        }
    }

    fun setAttendanceThresholdPercent(percent: Int) {
        viewModelScope.launch {
            settingsPreferences.setAttendanceThresholdPercent(percent)
        }
    }

    fun setCampusGapBufferMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsPreferences.setCampusGapBufferMinutes(minutes)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsPreferences.setThemeMode(mode)
        }
    }

    private suspend fun rescheduleAllRevisionReminders(context: Application) {
        val subjectsMap = repository.getAllSubjects().associateBy { it.id }
        val topics = repository.getAllTopicsSync()
        for (topic in topics) {
            if (topic.isCompleted && topic.nextReviewAt > 0L) {
                val subjectName = subjectsMap[topic.subjectId]?.name ?: "Study Subject"
                NotificationHelper.scheduleRevisionReminder(context, topic, subjectName)
            }
        }
    }

    private suspend fun cancelAllRevisionReminders(context: Application) {
        val topics = repository.getAllTopicsSync()
        for (topic in topics) {
            NotificationHelper.cancelRevisionReminder(context, topic.id)
        }
    }
}
