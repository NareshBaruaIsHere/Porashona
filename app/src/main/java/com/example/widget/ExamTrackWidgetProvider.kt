package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.ExamTrackDatabase
import com.example.data.model.ClassRoutineItem
import com.example.ui.routine.DayOfWeek
import com.example.util.CountdownStatus
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ExamTrackWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH_WIDGET,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ExamTrackWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = ExamTrackDatabase.getDatabase(context)
                val allSubjects = db.subjectDao().getAllSubjectsSync()
                val allRoutines = try {
                    db.classRoutineDao().getAllRoutinesSync()
                } catch (_: Exception) {
                    emptyList()
                }

                // 1. Next Upcoming Exam Calculation (respecting exam startTime)
                val nowMillis = System.currentTimeMillis()
                val upcomingSubject = allSubjects.firstOrNull { subj ->
                    DateUtils.getCountdownInfo(
                        examDateMillis = subj.examDate,
                        currentMillis = nowMillis,
                        startTime = subj.startTime
                    ).status != CountdownStatus.PASSED
                } ?: allSubjects.firstOrNull()

                val topics = if (upcomingSubject != null) {
                    db.topicDao().getTopicsForSubjectSync(upcomingSubject.id)
                } else {
                    emptyList()
                }

                val totalTopics = topics.size
                val doneTopics = topics.count { it.isDone }
                val progressPct = if (totalTopics > 0) ((doneTopics.toFloat() / totalTopics) * 100).toInt() else 0

                // 2. Live / Next Class Calculation (Today + Weekly Lookahead)
                val todayDay = DayOfWeek.fromCalendar(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
                val currentMinutes = DateUtils.getCurrentMinutesOfDay()
                val todaysRoutines = allRoutines.filter {
                    it.dayOfWeek.equals(todayDay.key, ignoreCase = true)
                }.sortedBy { DateUtils.parseTimeToMinutes(it.startTime) }

                var liveRoutine: ClassRoutineItem? = null
                var nextRoutineToday: ClassRoutineItem? = null

                for (routine in todaysRoutines) {
                    val startMin = DateUtils.parseTimeToMinutes(routine.startTime)
                    val isLive = DateUtils.isTimeBetween(routine.startTime, routine.endTime, currentMinutes)
                    if (isLive && liveRoutine == null) {
                        liveRoutine = routine
                    } else if (startMin > currentMinutes && nextRoutineToday == null) {
                        nextRoutineToday = routine
                    }
                }

                // Look ahead to the next day in the week if no more classes today
                var nextUpcomingRoutineOtherDay: Pair<DayOfWeek, ClassRoutineItem>? = null
                if (liveRoutine == null && nextRoutineToday == null && allRoutines.isNotEmpty()) {
                    val daysOrder = DayOfWeek.entries
                    val todayIdx = daysOrder.indexOf(todayDay).coerceAtLeast(0)
                    for (offset in 1..7) {
                        val candidateDay = daysOrder[(todayIdx + offset) % daysOrder.size]
                        val firstForDay = allRoutines
                            .filter { it.dayOfWeek.equals(candidateDay.key, ignoreCase = true) }
                            .minByOrNull { DateUtils.parseTimeToMinutes(it.startTime) }
                        if (firstForDay != null) {
                            nextUpcomingRoutineOtherDay = candidateDay to firstForDay
                            break
                        }
                    }
                }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_exam_track)
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120)
                    val isCompactHeight = minHeightDp in 1..95

                    // PendingIntent to launch app
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        if (upcomingSubject != null) {
                            putExtra("extra_subject_id", upcomingSubject.id)
                        }
                    }
                    val launchPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, launchPendingIntent)

                    // PendingIntent for real-time manual refresh button
                    val refreshIntent = Intent(context, ExamTrackWidgetProvider::class.java).apply {
                        action = ACTION_REFRESH_WIDGET
                    }
                    val refreshPendingIntent = PendingIntent.getBroadcast(
                        context,
                        appWidgetId + 1000,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)

                    // Populate Next Exam Section
                    if (upcomingSubject != null) {
                        val countdown = DateUtils.getCountdownInfo(
                            examDateMillis = upcomingSubject.examDate,
                            currentMillis = nowMillis,
                            startTime = upcomingSubject.startTime
                        )
                        views.setTextViewText(R.id.widget_countdown_number, countdown.shortLabel)
                        views.setTextViewText(R.id.widget_subject_name, upcomingSubject.name)
                        val timePart = if (upcomingSubject.startTime.isNotBlank()) " • ${upcomingSubject.startTime}" else ""
                        val roomPart = if (upcomingSubject.roomNumber.isNotBlank()) " • Rm ${upcomingSubject.roomNumber}" else ""
                        views.setTextViewText(
                            R.id.widget_exam_date,
                            DateUtils.formatExamDate(upcomingSubject.examDate) + timePart + roomPart
                        )

                        views.setViewVisibility(R.id.widget_progress_bar, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_progress_text, if (isCompactHeight) View.GONE else View.VISIBLE)
                        views.setProgressBar(R.id.widget_progress_bar, 100, progressPct, false)
                        views.setTextViewText(
                            R.id.widget_progress_text,
                            if (totalTopics > 0) "$doneTopics/$totalTopics topics ($progressPct%)" else "Tap to add syllabus topics"
                        )

                        when (countdown.status) {
                            CountdownStatus.TODAY -> views.setTextViewText(R.id.widget_label, "TODAY'S EXAM")
                            CountdownStatus.URGENT -> views.setTextViewText(R.id.widget_label, "UPCOMING EXAM")
                            else -> views.setTextViewText(R.id.widget_label, "NEXT EXAM")
                        }
                    } else {
                        views.setTextViewText(R.id.widget_label, "PORASHONA")
                        views.setTextViewText(R.id.widget_countdown_number, "No Exams")
                        views.setTextViewText(R.id.widget_subject_name, "Tap to add your exams")
                        views.setTextViewText(R.id.widget_exam_date, "Open app to schedule")
                        views.setViewVisibility(R.id.widget_progress_bar, View.GONE)
                        views.setViewVisibility(R.id.widget_progress_text, View.GONE)
                    }

                    // Populate Live / Next Class Section
                    if (isCompactHeight) {
                        views.setViewVisibility(R.id.widget_class_section, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_class_section, View.VISIBLE)
                        when {
                            liveRoutine != null -> {
                                val endMin = DateUtils.parseTimeToMinutes(liveRoutine.endTime)
                                val minsLeft = (endMin - currentMinutes).coerceAtLeast(1)
                                val title = if (liveRoutine.courseCode.isNotBlank()) {
                                    "${liveRoutine.courseCode} • ${liveRoutine.subjectName}"
                                } else {
                                    liveRoutine.subjectName
                                }
                                views.setTextViewText(R.id.widget_class_badge, "LIVE NOW")
                                views.setTextViewText(R.id.widget_class_title, title)
                                val roomPart = if (liveRoutine.roomNumber.isNotBlank()) " • ${liveRoutine.roomNumber}" else ""
                                val attPart = if (liveRoutine.totalConducted > 0) " • ${liveRoutine.attendancePercentage}% att" else ""
                                views.setTextViewText(
                                    R.id.widget_class_meta,
                                    "${liveRoutine.startTime}–${liveRoutine.endTime}$roomPart • ${minsLeft}m left$attPart"
                                )
                            }
                            nextRoutineToday != null -> {
                                val startMin = DateUtils.parseTimeToMinutes(nextRoutineToday.startTime)
                                val minsUntil = (startMin - currentMinutes).coerceAtLeast(1)
                                val untilLabel = if (minsUntil < 60) "in ${minsUntil}m" else "in ${minsUntil / 60}h ${minsUntil % 60}m"
                                val title = if (nextRoutineToday.courseCode.isNotBlank()) {
                                    "${nextRoutineToday.courseCode} • ${nextRoutineToday.subjectName}"
                                } else {
                                    nextRoutineToday.subjectName
                                }
                                views.setTextViewText(R.id.widget_class_badge, "NEXT CLASS")
                                views.setTextViewText(R.id.widget_class_title, title)
                                val roomPart = if (nextRoutineToday.roomNumber.isNotBlank()) " • ${nextRoutineToday.roomNumber}" else ""
                                val attPart = if (nextRoutineToday.totalConducted > 0) " • ${nextRoutineToday.attendancePercentage}%" else ""
                                views.setTextViewText(
                                    R.id.widget_class_meta,
                                    "${nextRoutineToday.startTime}$roomPart ($untilLabel)$attPart"
                                )
                            }
                            nextUpcomingRoutineOtherDay != null -> {
                                val (day, routine) = nextUpcomingRoutineOtherDay
                                val title = if (routine.courseCode.isNotBlank()) {
                                    "${routine.courseCode} • ${routine.subjectName}"
                                } else {
                                    routine.subjectName
                                }
                                views.setTextViewText(R.id.widget_class_badge, "NEXT • ${day.shortName.uppercase()}")
                                views.setTextViewText(R.id.widget_class_title, title)
                                val roomPart = if (routine.roomNumber.isNotBlank()) " • ${routine.roomNumber}" else ""
                                val attPart = if (routine.totalConducted > 0) " • ${routine.attendancePercentage}% att" else ""
                                views.setTextViewText(
                                    R.id.widget_class_meta,
                                    "${routine.startTime}–${routine.endTime}$roomPart$attPart"
                                )
                            }
                            else -> {
                                views.setTextViewText(R.id.widget_class_badge, todayDay.shortName.uppercase())
                                views.setTextViewText(R.id.widget_class_title, "No classes scheduled")
                                views.setTextViewText(R.id.widget_class_meta, "Tap to add your weekly routine")
                            }
                        }
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
