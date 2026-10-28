package com.example.ui.study

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExamTrackApplication
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class StudyDayStats(
    val dateMillis: Long,
    val dayLabel: String, // "Mon", "Tue", etc.
    val dayNumber: String, // "11"
    val totalMinutes: Int,
    val isToday: Boolean
)

data class SubjectStudyStats(
    val subjectId: String?,
    val subjectName: String,
    val colorHex: String,
    val totalMinutes: Int,
    val percentage: Float
)

data class StudyTrackerUiState(
    val todayMinutes: Int = 0,
    val weeklyMinutes: Int = 0,
    val totalMinutes: Int = 0,
    val streakDays: Int = 0,
    val dailyAverageMinutes: Int = 0,
    val past7Days: List<StudyDayStats> = emptyList(),
    val subjectBreakdown: List<SubjectStudyStats> = emptyList(),
    val recentSessions: List<StudySession> = emptyList(),
    val availableSubjects: List<Subject> = emptyList(),
    val isLoading: Boolean = false
) {
    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    val todayFormatted: String get() = formatDuration(todayMinutes)
    val weeklyFormatted: String get() = formatDuration(weeklyMinutes)
    val dailyAverageFormatted: String get() = formatDuration(dailyAverageMinutes)
}

class StudyTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ExamTrackApplication).repository

    val uiState: StateFlow<StudyTrackerUiState> = combine(
        repository.allStudySessions,
        repository.allSubjectsWithTopics
    ) { sessions, subjectsWithTopics ->
        val subjects = subjectsWithTopics.map { it.subject }
        calculateStats(sessions, subjects)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudyTrackerUiState(isLoading = true)
    )

    private fun calculateStats(
        sessions: List<StudySession>,
        subjects: List<Subject>
    ): StudyTrackerUiState {
        val now = System.currentTimeMillis()
        val todayStart = DateUtils.startOfDay(now)
        val todayEnd = todayStart + TimeUnit.DAYS.toMillis(1)

        // Today's total
        val todayMinutes = sessions
            .filter { it.dateMillis in todayStart until todayEnd }
            .sumOf { it.durationMinutes }

        // Past 7 Days calculation (from 6 days ago up to today)
        val dayStats = mutableListOf<StudyDayStats>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())

        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            cal.timeInMillis = todayStart - TimeUnit.DAYS.toMillis(i.toLong())
            val startOfTargetDay = cal.timeInMillis
            val endOfTargetDay = startOfTargetDay + TimeUnit.DAYS.toMillis(1)

            val minutesInDay = sessions
                .filter { it.dateMillis in startOfTargetDay until endOfTargetDay }
                .sumOf { it.durationMinutes }

            dayStats.add(
                StudyDayStats(
                    dateMillis = startOfTargetDay,
                    dayLabel = dayFormat.format(Date(startOfTargetDay)),
                    dayNumber = dayNumFormat.format(Date(startOfTargetDay)),
                    totalMinutes = minutesInDay,
                    isToday = (i == 0)
                )
            )
        }

        // Weekly Minutes (Sum of past 7 days)
        val weeklyMinutes = dayStats.sumOf { it.totalMinutes }
        val dailyAverageMinutes = if (dayStats.isNotEmpty()) weeklyMinutes / dayStats.size else 0

        // Total all time
        val totalMinutes = sessions.sumOf { it.durationMinutes }

        // Calculate Study Streak (consecutive days with > 0 minutes)
        var streak = 0
        var checkCal = Calendar.getInstance()
        checkCal.timeInMillis = todayStart

        // Check if studied today
        val studiedToday = sessions.any { it.dateMillis in todayStart until todayEnd }
        if (!studiedToday) {
            // Check if studied yesterday to keep streak active
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStart = checkCal.timeInMillis
            val yesterdayEnd = yesterdayStart + TimeUnit.DAYS.toMillis(1)
            val studiedYesterday = sessions.any { it.dateMillis in yesterdayStart until yesterdayEnd }
            if (studiedYesterday) {
                streak = 1
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
                while (true) {
                    val s = checkCal.timeInMillis
                    val e = s + TimeUnit.DAYS.toMillis(1)
                    if (sessions.any { it.dateMillis in s until e }) {
                        streak++
                        checkCal.add(Calendar.DAY_OF_YEAR, -1)
                    } else {
                        break
                    }
                }
            }
        } else {
            streak = 1
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            while (true) {
                val s = checkCal.timeInMillis
                val e = s + TimeUnit.DAYS.toMillis(1)
                if (sessions.any { it.dateMillis in s until e }) {
                    streak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        // Subject Breakdown
        val sessionsBySubject = sessions.groupBy { it.subjectName }
        val subjectStats = sessionsBySubject.map { (name, list) ->
            val totalForSub = list.sumOf { it.durationMinutes }
            val color = list.firstOrNull()?.subjectColor ?: "#0D9488"
            val pct = if (totalMinutes > 0) totalForSub.toFloat() / totalMinutes.toFloat() else 0f
            SubjectStudyStats(
                subjectId = list.firstOrNull()?.subjectId,
                subjectName = name,
                colorHex = color,
                totalMinutes = totalForSub,
                percentage = pct
            )
        }.sortedByDescending { it.totalMinutes }

        return StudyTrackerUiState(
            todayMinutes = todayMinutes,
            weeklyMinutes = weeklyMinutes,
            totalMinutes = totalMinutes,
            streakDays = streak,
            dailyAverageMinutes = dailyAverageMinutes,
            past7Days = dayStats,
            subjectBreakdown = subjectStats,
            recentSessions = sessions,
            availableSubjects = subjects,
            isLoading = false
        )
    }

    fun logStudySession(
        subjectId: String?,
        subjectName: String,
        subjectColor: String,
        durationMinutes: Int,
        notes: String,
        dateMillis: Long = System.currentTimeMillis(),
        sessionType: String = "MANUAL"
    ) {
        if (durationMinutes <= 0) return
        viewModelScope.launch {
            val session = StudySession(
                subjectId = subjectId,
                subjectName = subjectName,
                subjectColor = subjectColor,
                durationMinutes = durationMinutes,
                notes = notes.trim(),
                dateMillis = dateMillis,
                sessionType = sessionType
            )
            repository.insertStudySession(session)
        }
    }

    fun deleteStudySession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteStudySessionById(sessionId)
        }
    }
}
