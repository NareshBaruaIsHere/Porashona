package com.example.ui.pomodoro

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExamTrackApplication
import com.example.data.model.ExamReadinessResult
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.calculateSmartReadiness
import com.example.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class PomodoroMode(val defaultMinutes: Int, val label: String) {
    FOCUS(30, "Focus"),
    SHORT_BREAK(5, "Break"),
    LONG_BREAK(15, "Long Break")
}

enum class PomodoroStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class SubjectFocusTarget(
    val subject: Subject,
    val readiness: ExamReadinessResult,
    val loggedMinutesToday: Int,
    val totalLoggedMinutes: Int,
    val plannedTargetMinutes: Int,
    val daysRemaining: Int
)

data class PomodoroUiState(
    val mode: PomodoroMode = PomodoroMode.FOCUS,
    val status: PomodoroStatus = PomodoroStatus.IDLE,
    val totalSeconds: Int = 30 * 60,
    val secondsRemaining: Int = 30 * 60,
    val selectedSubjectId: String? = null,
    val selectedSubjectName: String = "General Study",
    val selectedSubjectColor: String = "#0D9488",
    val completedSessionsToday: Int = 0,
    val completedMinutesToday: Int = 0,
    val dailyTargetMinutes: Int = 120,
    val defaultFocusMinutes: Int = 30,
    val defaultBreakMinutes: Int = 5,
    val examModeEnabled: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val focusIntegrityStreak: Int = 0,
    val totalLockdownSessionsCompleted: Int = 0,
    val showStartLockdownPrompt: Boolean = false,
    val isLockdownActive: Boolean = false,
    val lastSessionWasLockdown: Boolean = false,
    val streakBrokenAlert: Boolean = false,
    val lostStreakCount: Int = 0,
    val availableSubjects: List<Subject> = emptyList(),
    val subjectFocusTargets: List<SubjectFocusTarget> = emptyList(),
    val recentCompletedSessions: List<StudySession> = emptyList(),
    val showCelebrationDialog: Boolean = false,
    val lastCompletedMinutes: Int = 30,
    val completionFeedbackPulse: Int = 0
) {
    val progress: Float
        get() = if (totalSeconds > 0) {
            (totalSeconds - secondsRemaining).toFloat() / totalSeconds.toFloat()
        } else 0f

    val dailyTargetProgress: Float
        get() = if (dailyTargetMinutes > 0) {
            (completedMinutesToday.toFloat() / dailyTargetMinutes.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val remainingDailyMinutes: Int
        get() = maxOf(0, dailyTargetMinutes - completedMinutesToday)

    val plannedSessionMinutes: Int
        get() = totalSeconds / 60

    val selectedSubjectTarget: SubjectFocusTarget?
        get() = subjectFocusTargets.firstOrNull { it.subject.id == selectedSubjectId }

    val formattedTime: String
        get() {
            val mins = secondsRemaining / 60
            val secs = secondsRemaining % 60
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
        }
}

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ExamTrackApplication).repository
    private val settingsPreferences = (application as ExamTrackApplication).settingsPreferences
    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var hasInitializedDefaultDuration = false

    init {
        // Observe subjects + routine classes + study sessions together so all subjects appear in the Focus selector
        viewModelScope.launch {
            combine(
                repository.allSubjectsWithTopics,
                repository.allStudySessions,
                repository.allClassRoutines
            ) { subjectsWithTopics, sessions, routines ->
                Triple(subjectsWithTopics, sessions, routines)
            }.collect { (subjectsWithTopics, sessions, routines) ->
                val now = System.currentTimeMillis()
                val todayStart = DateUtils.startOfDay(now)
                val todayEnd = todayStart + TimeUnit.DAYS.toMillis(1)

                val todaySessions = sessions.filter { it.dateMillis in todayStart until todayEnd }
                val pomoCountToday = todaySessions.count { it.sessionType == "POMODORO" }
                val totalFocusMinsToday = todaySessions.sumOf { it.durationMinutes }

                val examSubjects = subjectsWithTopics.map { it.subject }
                val examNamesLower = examSubjects.map { it.name.trim().lowercase() }.toSet()

                // Also include unique subjects from the user's Class Routine that aren't already in Exams
                val routineOnlySubjects = routines
                    .distinctBy { it.subjectName.trim().lowercase() }
                    .filter { it.subjectName.isNotBlank() && it.subjectName.trim().lowercase() !in examNamesLower }
                    .map { routine ->
                        Subject(
                            id = "routine_${routine.subjectName.trim().lowercase()}",
                            name = routine.subjectName.trim(),
                            examDate = now,
                            colorTag = routine.colorTag,
                            roomNumber = routine.roomNumber
                        )
                    }

                val subjects = examSubjects + routineOnlySubjects

                val targets = subjectsWithTopics.map { swt ->
                    val subjectSessions = sessions.filter {
                        it.subjectId == swt.subject.id || it.subjectName.equals(swt.subject.name, ignoreCase = true)
                    }
                    val totalSubjMins = subjectSessions.sumOf { it.durationMinutes }
                    val todaySubjMins = todaySessions
                        .filter {
                            it.subjectId == swt.subject.id || it.subjectName.equals(swt.subject.name, ignoreCase = true)
                        }
                        .sumOf { it.durationMinutes }
                    val readiness = swt.calculateSmartReadiness(
                        actualFocusMinutes = totalSubjMins,
                        nowMillis = now
                    )
                    SubjectFocusTarget(
                        subject = swt.subject,
                        readiness = readiness,
                        loggedMinutesToday = todaySubjMins,
                        totalLoggedMinutes = totalSubjMins,
                        plannedTargetMinutes = readiness.expectedFocusMinutes,
                        daysRemaining = readiness.daysRemaining
                    )
                }.sortedWith(
                    compareBy<SubjectFocusTarget> { if (it.daysRemaining < 0) 1 else 0 }
                        .thenBy { if (it.daysRemaining >= 0) it.daysRemaining else Int.MAX_VALUE }
                        .thenBy { it.readiness.score }
                )

                _uiState.update { current ->
                    val resolvedSelectedSubject = subjects.firstOrNull { it.id == current.selectedSubjectId }
                    current.copy(
                        availableSubjects = subjects,
                        subjectFocusTargets = targets,
                        completedSessionsToday = pomoCountToday,
                        completedMinutesToday = totalFocusMinsToday,
                        recentCompletedSessions = sessions.take(6),
                        selectedSubjectName = resolvedSelectedSubject?.name
                            ?: if (current.selectedSubjectId == null) "General Study" else current.selectedSubjectName,
                        selectedSubjectColor = resolvedSelectedSubject?.colorTag
                            ?: if (current.selectedSubjectId == null) "#0D9488" else current.selectedSubjectColor
                    )
                }
            }
        }

        // Observe persisted Pomodoro, Exam Mode & Haptic preferences
        viewModelScope.launch {
            combine(
                settingsPreferences.dailyFocusTargetMinutes,
                settingsPreferences.defaultFocusMinutes,
                settingsPreferences.defaultBreakMinutes,
                settingsPreferences.examModeEnabled,
                settingsPreferences.hapticFeedbackEnabled
            ) { dailyTarget, defFocus, defBreak, examMode, haptics ->
                listOf(dailyTarget, defFocus, defBreak, if (examMode) 1 else 0, if (haptics) 1 else 0)
            }.collect { values ->
                val dailyTarget = values[0]
                val defFocus = values[1]
                val defBreak = values[2]
                val examMode = values[3] == 1
                val haptics = values[4] == 1

                _uiState.update { current ->
                    val shouldUpdateIdleTimer = !hasInitializedDefaultDuration && current.status == PomodoroStatus.IDLE
                    hasInitializedDefaultDuration = true
                    val newTotalSec = if (shouldUpdateIdleTimer) {
                        when (current.mode) {
                            PomodoroMode.FOCUS -> defFocus * 60
                            PomodoroMode.SHORT_BREAK -> defBreak * 60
                            PomodoroMode.LONG_BREAK -> PomodoroMode.LONG_BREAK.defaultMinutes * 60
                        }
                    } else {
                        current.totalSeconds
                    }
                    current.copy(
                        dailyTargetMinutes = dailyTarget,
                        defaultFocusMinutes = defFocus,
                        defaultBreakMinutes = defBreak,
                        examModeEnabled = examMode,
                        hapticFeedbackEnabled = haptics,
                        totalSeconds = newTotalSec,
                        secondsRemaining = if (shouldUpdateIdleTimer) newTotalSec else current.secondsRemaining
                    )
                }
            }
        }

        // Observe Focus Integrity Streak & Total Lockdown Sessions
        viewModelScope.launch {
            combine(
                settingsPreferences.focusIntegrityStreak,
                settingsPreferences.totalLockdownSessionsCompleted
            ) { streak, totalCompleted ->
                Pair(streak, totalCompleted)
            }.collect { (streak, totalCompleted) ->
                _uiState.update { current ->
                    current.copy(
                        focusIntegrityStreak = streak,
                        totalLockdownSessionsCompleted = totalCompleted
                    )
                }
            }
        }
    }

    fun selectSubject(subject: Subject?) {
        if (subject != null) {
            _uiState.update {
                it.copy(
                    selectedSubjectId = subject.id,
                    selectedSubjectName = subject.name,
                    selectedSubjectColor = subject.colorTag
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedSubjectId = null,
                    selectedSubjectName = "General Study",
                    selectedSubjectColor = "#0D9488"
                )
            }
        }
    }

    fun selectSubjectById(subjectId: String?) {
        if (subjectId == null) {
            selectSubject(null)
            return
        }
        val found = _uiState.value.availableSubjects.firstOrNull { it.id == subjectId }
        if (found != null) {
            selectSubject(found)
        } else {
            _uiState.update { it.copy(selectedSubjectId = subjectId) }
        }
    }

    fun setMode(mode: PomodoroMode, customMinutes: Int? = null) {
        timerJob?.cancel()
        val current = _uiState.value
        val defaultForMode = when (mode) {
            PomodoroMode.FOCUS -> current.defaultFocusMinutes
            PomodoroMode.SHORT_BREAK -> current.defaultBreakMinutes
            PomodoroMode.LONG_BREAK -> PomodoroMode.LONG_BREAK.defaultMinutes
        }
        val durationMins = customMinutes ?: defaultForMode
        val totalSec = durationMins * 60
        _uiState.update {
            it.copy(
                mode = mode,
                status = PomodoroStatus.IDLE,
                isLockdownActive = false,
                showStartLockdownPrompt = false,
                totalSeconds = totalSec,
                secondsRemaining = totalSec
            )
        }
    }

    fun setCustomFocusMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 240)
        if (_uiState.value.mode == PomodoroMode.FOCUS) {
            setMode(PomodoroMode.FOCUS, clamped)
        }
        viewModelScope.launch {
            settingsPreferences.setDefaultFocusMinutes(clamped)
        }
    }

    fun setCustomBreakMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 60)
        if (_uiState.value.mode != PomodoroMode.FOCUS) {
            setMode(_uiState.value.mode, clamped)
        }
        viewModelScope.launch {
            settingsPreferences.setDefaultBreakMinutes(clamped)
        }
    }

    fun updateDailyFocusTarget(minutes: Int) {
        val clamped = minutes.coerceIn(15, 720)
        viewModelScope.launch {
            settingsPreferences.setDailyFocusTargetMinutes(clamped)
        }
    }

    /**
     * Used by Campus Gap Focus Mode and Exam Mode quick actions to pre-configure and optionally
     * start a focused session for a specific duration and subject.
     */
    fun launchQuickGapSession(
        durationMinutes: Int,
        subjectId: String?,
        subjectName: String? = null,
        subjectColor: String? = null,
        autoStart: Boolean = true
    ) {
        timerJob?.cancel()
        val foundSubject = subjectId?.let { id ->
            _uiState.value.availableSubjects.firstOrNull { it.id == id }
        }
        val clampedMins = durationMinutes.coerceIn(5, 180)
        val totalSec = clampedMins * 60
        _uiState.update { current ->
            current.copy(
                mode = PomodoroMode.FOCUS,
                status = PomodoroStatus.IDLE,
                isLockdownActive = false,
                showStartLockdownPrompt = false,
                totalSeconds = totalSec,
                secondsRemaining = totalSec,
                selectedSubjectId = foundSubject?.id ?: subjectId,
                selectedSubjectName = foundSubject?.name ?: subjectName ?: "General Study",
                selectedSubjectColor = foundSubject?.colorTag ?: subjectColor ?: "#0D9488"
            )
        }
        if (autoStart) {
            startTimer()
        }
    }

    /**
     * Invoked when the user presses the Play button in the UI.
     * In Focus mode when IDLE, asks whether to enable Focus Lockdown (OLED Zen Sanctuary).
     * When resuming from PAUSED or starting a Break, starts immediately.
     */
    fun onPlayButtonClicked() {
        val current = _uiState.value
        if (current.status == PomodoroStatus.RUNNING) {
            pauseTimer()
        } else if (current.mode == PomodoroMode.FOCUS && current.status == PomodoroStatus.IDLE) {
            _uiState.update { it.copy(showStartLockdownPrompt = true) }
        } else {
            startTimer()
        }
    }

    fun dismissStartLockdownPrompt() {
        _uiState.update { it.copy(showStartLockdownPrompt = false) }
    }

    fun startFocusWithChoice(enableLockdown: Boolean) {
        _uiState.update {
            it.copy(
                showStartLockdownPrompt = false,
                isLockdownActive = enableLockdown,
                streakBrokenAlert = false
            )
        }
        startTimer()
    }

    /**
     * Breaks an active Focus Lockdown early: cancels the timer, resets the Focus Integrity Streak
     * to 0 as a penalty, and exits the OLED Zen Sanctuary.
     */
    fun breakLockdownEarly() {
        val lostStreak = _uiState.value.focusIntegrityStreak
        timerJob?.cancel()
        viewModelScope.launch {
            settingsPreferences.resetFocusIntegrityStreak()
        }
        vibratePhone()
        _uiState.update {
            it.copy(
                status = PomodoroStatus.IDLE,
                isLockdownActive = false,
                showStartLockdownPrompt = false,
                secondsRemaining = it.totalSeconds,
                streakBrokenAlert = true,
                lostStreakCount = lostStreak
            )
        }
    }

    fun dismissStreakBrokenAlert() {
        _uiState.update { it.copy(streakBrokenAlert = false) }
    }

    fun startTimer() {
        if (_uiState.value.status == PomodoroStatus.RUNNING) return

        _uiState.update { it.copy(status = PomodoroStatus.RUNNING, showStartLockdownPrompt = false) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val currentRemaining = _uiState.value.secondsRemaining
                if (currentRemaining > 1) {
                    _uiState.update { it.copy(secondsRemaining = it.secondsRemaining - 1) }
                } else {
                    // Timer finished
                    _uiState.update { it.copy(secondsRemaining = 0) }
                    handleTimerComplete()
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(status = PomodoroStatus.PAUSED) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                status = PomodoroStatus.IDLE,
                isLockdownActive = false,
                showStartLockdownPrompt = false,
                secondsRemaining = it.totalSeconds
            )
        }
    }

    /**
     * Stops the timer at any time and automatically logs any elapsed study time (1 min or more)
     * to the database so that it reflects on the dashboard and exam readiness immediately.
     */
    fun stopAndSaveTimer() {
        val currentState = _uiState.value
        timerJob?.cancel()

        val elapsedSeconds = currentState.totalSeconds - currentState.secondsRemaining
        // Minimum 45s rounds to 1 minute, ensuring studying for 1 minute counts
        val elapsedMinutes = if (elapsedSeconds >= 45) {
            maxOf(1, (elapsedSeconds + 30) / 60)
        } else {
            0
        }

        if (currentState.mode == PomodoroMode.FOCUS && elapsedMinutes >= 1) {
            viewModelScope.launch {
                val session = StudySession(
                    subjectId = currentState.selectedSubjectId,
                    subjectName = currentState.selectedSubjectName,
                    subjectColor = currentState.selectedSubjectColor,
                    durationMinutes = elapsedMinutes,
                    notes = "Pomodoro focus session ($elapsedMinutes min)",
                    dateMillis = System.currentTimeMillis(),
                    sessionType = "POMODORO"
                )
                repository.insertStudySession(session)
            }
            vibratePhone()
            _uiState.update {
                it.copy(
                    status = PomodoroStatus.IDLE,
                    isLockdownActive = false,
                    secondsRemaining = it.totalSeconds,
                    showCelebrationDialog = true,
                    lastSessionWasLockdown = false,
                    lastCompletedMinutes = elapsedMinutes
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    status = PomodoroStatus.IDLE,
                    isLockdownActive = false,
                    secondsRemaining = it.totalSeconds
                )
            }
        }
    }

    fun skip() {
        timerJob?.cancel()
        val nextMode = when (_uiState.value.mode) {
            PomodoroMode.FOCUS -> PomodoroMode.SHORT_BREAK
            PomodoroMode.SHORT_BREAK, PomodoroMode.LONG_BREAK -> PomodoroMode.FOCUS
        }
        setMode(nextMode)
    }

    private fun handleTimerComplete() {
        val currentState = _uiState.value
        val completedDurationMins = maxOf(1, currentState.totalSeconds / 60)
        val wasLockdown = currentState.isLockdownActive

        // Trigger haptic vibration feedback and completion tone for both Focus and Break modes
        vibratePhone(isTimerCompletion = true)
        playCompletionTone(isFocusMode = currentState.mode == PomodoroMode.FOCUS)

        if (currentState.mode == PomodoroMode.FOCUS) {
            // Automatically log study session & increment Focus Integrity Streak if in Lockdown
            viewModelScope.launch {
                if (wasLockdown) {
                    settingsPreferences.incrementFocusIntegrityStreak()
                }
                val session = StudySession(
                    subjectId = currentState.selectedSubjectId,
                    subjectName = currentState.selectedSubjectName,
                    subjectColor = currentState.selectedSubjectColor,
                    durationMinutes = completedDurationMins,
                    notes = if (wasLockdown) {
                        "Completed Lockdown Sanctuary session"
                    } else {
                        "Completed Pomodoro focus session"
                    },
                    dateMillis = System.currentTimeMillis(),
                    sessionType = "POMODORO"
                )
                repository.insertStudySession(session)
            }

            _uiState.update {
                it.copy(
                    status = PomodoroStatus.COMPLETED,
                    isLockdownActive = false,
                    lastSessionWasLockdown = wasLockdown,
                    showCelebrationDialog = true,
                    lastCompletedMinutes = completedDurationMins,
                    completionFeedbackPulse = it.completionFeedbackPulse + 1
                )
            }
        } else {
            // Break finished
            _uiState.update {
                it.copy(
                    status = PomodoroStatus.COMPLETED,
                    isLockdownActive = false,
                    showCelebrationDialog = false,
                    completionFeedbackPulse = it.completionFeedbackPulse + 1
                )
            }
        }
    }

    fun dismissCelebrationDialog() {
        _uiState.update { it.copy(showCelebrationDialog = false) }
    }

    private fun vibratePhone(isTimerCompletion: Boolean = false) {
        if (!_uiState.value.hapticFeedbackEnabled) return
        try {
            val context = getApplication<Application>()
            val pattern = if (isTimerCompletion) {
                longArrayOf(0, 220, 120, 280, 120, 420)
            } else {
                longArrayOf(0, 300, 200, 400)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(if (isTimerCompletion) 700L else 500L)
                    }
                }
            }
        } catch (e: Exception) {
            // Non-critical vibration fallback
        }
    }

    private fun playCompletionTone(isFocusMode: Boolean) {
        viewModelScope.launch {
            var toneGenerator: ToneGenerator? = null
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                val firstTone = if (isFocusMode) {
                    ToneGenerator.TONE_PROP_ACK
                } else {
                    ToneGenerator.TONE_PROP_PROMPT
                }
                toneGenerator.startTone(firstTone, 220)
                delay(260)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 300)
                delay(350)
            } catch (_: Exception) {
                try {
                    val context = getApplication<Application>()
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    RingtoneManager.getRingtone(context, uri)?.play()
                } catch (_: Exception) {
                    // Ignore audio fallback failure in headless/test environments
                }
            } finally {
                try {
                    toneGenerator?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
