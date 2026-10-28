package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "examtrack_settings")

class SettingsPreferences(private val context: Context) {

    private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val EXAM_MODE_ENABLED_KEY = booleanPreferencesKey("exam_mode_enabled")
    private val REVISION_REMINDERS_ENABLED_KEY = booleanPreferencesKey("revision_reminders_enabled")
    private val READINESS_ALERTS_ENABLED_KEY = booleanPreferencesKey("readiness_alerts_enabled")
    private val DAILY_FOCUS_TARGET_MINUTES_KEY = intPreferencesKey("daily_focus_target_minutes")
    private val DEFAULT_FOCUS_MINUTES_KEY = intPreferencesKey("default_focus_minutes")
    private val DEFAULT_BREAK_MINUTES_KEY = intPreferencesKey("default_break_minutes")
    private val ATTENDANCE_THRESHOLD_PERCENT_KEY = intPreferencesKey("attendance_threshold_percent")
    private val CAMPUS_GAP_BUFFER_MINUTES_KEY = intPreferencesKey("campus_gap_buffer_minutes")
    private val DISMISSED_CAMPUS_GAPS_KEY = stringSetPreferencesKey("dismissed_campus_gaps")
    private val HAPTIC_FEEDBACK_ENABLED_KEY = booleanPreferencesKey("haptic_feedback_enabled")
    private val FOCUS_INTEGRITY_STREAK_KEY = intPreferencesKey("focus_integrity_streak")
    private val TOTAL_LOCKDOWN_SESSIONS_KEY = intPreferencesKey("total_lockdown_sessions")
    private val DAILY_STUDY_REMINDER_ENABLED_KEY = booleanPreferencesKey("daily_study_reminder_enabled")
    private val DAILY_STUDY_REMINDER_HOUR_KEY = intPreferencesKey("daily_study_reminder_hour")
    private val DAILY_STUDY_REMINDER_MINUTE_KEY = intPreferencesKey("daily_study_reminder_minute")
    private val NOTIFICATION_PERMISSION_REQUESTED_KEY = booleanPreferencesKey("notification_permission_requested")

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
    }

    val notificationPermissionRequested: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_PERMISSION_REQUESTED_KEY] ?: false
    }

    val dailyStudyReminderEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DAILY_STUDY_REMINDER_ENABLED_KEY] ?: true
    }

    val dailyStudyReminderHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_STUDY_REMINDER_HOUR_KEY] ?: 20
    }

    val dailyStudyReminderMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_STUDY_REMINDER_MINUTE_KEY] ?: 0
    }

    val hapticFeedbackEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAPTIC_FEEDBACK_ENABLED_KEY] ?: true
    }

    val focusIntegrityStreak: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[FOCUS_INTEGRITY_STREAK_KEY] ?: 0
    }

    val totalLockdownSessionsCompleted: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TOTAL_LOCKDOWN_SESSIONS_KEY] ?: 0
    }

    val examModeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[EXAM_MODE_ENABLED_KEY] ?: false
    }

    val revisionRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REVISION_REMINDERS_ENABLED_KEY] ?: true
    }

    val readinessAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[READINESS_ALERTS_ENABLED_KEY] ?: true
    }

    val dailyFocusTargetMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_FOCUS_TARGET_MINUTES_KEY] ?: 120
    }

    val defaultFocusMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_FOCUS_MINUTES_KEY] ?: 30
    }

    val defaultBreakMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_BREAK_MINUTES_KEY] ?: 5
    }

    val attendanceThresholdPercent: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ATTENDANCE_THRESHOLD_PERCENT_KEY] ?: 80
    }

    val campusGapBufferMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CAMPUS_GAP_BUFFER_MINUTES_KEY] ?: 10
    }

    val dismissedCampusGaps: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[DISMISSED_CAMPUS_GAPS_KEY] ?: emptySet()
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val modeStr = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setNotificationPermissionRequested(requested: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_PERMISSION_REQUESTED_KEY] = requested
        }
    }

    suspend fun setAllReminderToggles(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_STUDY_REMINDER_ENABLED_KEY] = enabled
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
            preferences[REVISION_REMINDERS_ENABLED_KEY] = enabled
            preferences[READINESS_ALERTS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setDailyStudyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_STUDY_REMINDER_ENABLED_KEY] = enabled
        }
    }

    suspend fun setDailyStudyReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_STUDY_REMINDER_HOUR_KEY] = hour.coerceIn(0, 23)
            preferences[DAILY_STUDY_REMINDER_MINUTE_KEY] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setExamModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EXAM_MODE_ENABLED_KEY] = enabled
        }
    }

    suspend fun setRevisionRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REVISION_REMINDERS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setReadinessAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[READINESS_ALERTS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setDailyFocusTargetMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_FOCUS_TARGET_MINUTES_KEY] = minutes.coerceIn(15, 720)
        }
    }

    suspend fun setDefaultFocusMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_FOCUS_MINUTES_KEY] = minutes.coerceIn(5, 180)
        }
    }

    suspend fun setDefaultBreakMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_BREAK_MINUTES_KEY] = minutes.coerceIn(1, 60)
        }
    }

    suspend fun setAttendanceThresholdPercent(percent: Int) {
        context.dataStore.edit { preferences ->
            preferences[ATTENDANCE_THRESHOLD_PERCENT_KEY] = percent.coerceIn(50, 95)
        }
    }

    suspend fun setCampusGapBufferMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[CAMPUS_GAP_BUFFER_MINUTES_KEY] = minutes.coerceIn(0, 30)
        }
    }

    suspend fun dismissCampusGap(gapId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[DISMISSED_CAMPUS_GAPS_KEY] ?: emptySet()
            preferences[DISMISSED_CAMPUS_GAPS_KEY] = current + gapId
        }
    }

    suspend fun restoreDismissedCampusGap(gapId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[DISMISSED_CAMPUS_GAPS_KEY] ?: emptySet()
            preferences[DISMISSED_CAMPUS_GAPS_KEY] = current - gapId
        }
    }

    suspend fun clearDismissedCampusGaps() {
        context.dataStore.edit { preferences ->
            preferences[DISMISSED_CAMPUS_GAPS_KEY] = emptySet()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_ENABLED_KEY] = enabled
        }
    }

    suspend fun incrementFocusIntegrityStreak() {
        context.dataStore.edit { preferences ->
            val currentStreak = preferences[FOCUS_INTEGRITY_STREAK_KEY] ?: 0
            val currentTotal = preferences[TOTAL_LOCKDOWN_SESSIONS_KEY] ?: 0
            preferences[FOCUS_INTEGRITY_STREAK_KEY] = currentStreak + 1
            preferences[TOTAL_LOCKDOWN_SESSIONS_KEY] = currentTotal + 1
        }
    }

    suspend fun resetFocusIntegrityStreak() {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_INTEGRITY_STREAK_KEY] = 0
        }
    }
}
