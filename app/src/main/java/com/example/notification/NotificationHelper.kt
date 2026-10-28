package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.receiver.ExamNotificationReceiver
import com.example.util.DateUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationHelper {

    const val CHANNEL_ID = "exam_reminders_channel"
    const val REVISION_CHANNEL_ID = "topic_revision_channel"
    const val DAILY_STUDY_CHANNEL_ID = "daily_study_reminder_channel"

    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
    const val TYPE_EXAM_REMINDER = "EXAM_REMINDER"
    const val TYPE_REVISION_REMINDER = "REVISION_REMINDER"
    const val TYPE_DAILY_STUDY_REMINDER = "DAILY_STUDY_REMINDER"

    const val EXTRA_SUBJECT_ID = "extra_subject_id"
    const val EXTRA_SUBJECT_NAME = "extra_subject_name"
    const val EXTRA_DAYS_LEFT = "extra_days_left"
    const val EXTRA_TOPIC_ID = "extra_topic_id"
    const val EXTRA_TOPIC_TITLE = "extra_topic_title"
    const val EXTRA_REVIEW_STAGE = "extra_review_stage"
    const val EXTRA_DAILY_HOUR = "extra_daily_hour"
    const val EXTRA_DAILY_MINUTE = "extra_daily_minute"
    private const val DAILY_STUDY_REQUEST_CODE = 999_001

    private val REMINDER_DAYS = listOf(7, 3, 1)

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val examChannel = NotificationChannel(
                CHANNEL_ID,
                "Exam Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Study reminders 7, 3, and 1 day before upcoming exams"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(examChannel)

            val revisionChannel = NotificationChannel(
                REVISION_CHANNEL_ID,
                "Spaced-Repetition Reviews",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "3-day and 7-day Ebbinghaus topic revision reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(revisionChannel)

            val dailyStudyChannel = NotificationChannel(
                DAILY_STUDY_CHANNEL_ID,
                "Daily Study Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily study habit and streak reminder"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(dailyStudyChannel)
        }
    }

    fun scheduleExamReminders(context: Context, subject: Subject) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val examStartOfDay = DateUtils.startOfDay(subject.examDate)

        for (daysBefore in REMINDER_DAYS) {
            val reminderCal = Calendar.getInstance().apply {
                timeInMillis = examStartOfDay - TimeUnit.DAYS.toMillis(daysBefore.toLong())
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val triggerTime = reminderCal.timeInMillis
            if (triggerTime > System.currentTimeMillis()) {
                val intent = Intent(context, ExamNotificationReceiver::class.java).apply {
                    putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_EXAM_REMINDER)
                    putExtra(EXTRA_SUBJECT_ID, subject.id)
                    putExtra(EXTRA_SUBJECT_NAME, subject.name)
                    putExtra(EXTRA_DAYS_LEFT, daysBefore)
                }

                val requestCode = getRequestCode(subject.id, daysBefore)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleAlarmSafely(alarmManager, triggerTime, pendingIntent)
            }
        }
    }

    fun cancelExamReminders(context: Context, subjectId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (daysBefore in REMINDER_DAYS) {
            val intent = Intent(context, ExamNotificationReceiver::class.java)
            val requestCode = getRequestCode(subjectId, daysBefore)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    /**
     * Schedules Ebbinghaus spaced-repetition reminders for a completed topic.
     * Prevents duplicate reminders by using deterministic request codes per topicId + stage
     * and canceling any existing pending intents before scheduling.
     */
    fun scheduleTopicRevisionReminders(
        context: Context,
        subjectName: String,
        topic: Topic,
        firstIntervalDays: Int = 3,
        secondIntervalDays: Int = 7
    ) {
        cancelTopicRevisionReminders(context, topic.id)
        if (!topic.isDone || topic.reviewCount >= 2) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val baseTime = if (topic.lastReviewedAt > 0L) topic.lastReviewedAt else topic.completedAt
        if (baseTime <= 0L) return

        if (topic.reviewCount == 0) {
            // Schedule 1st revision (default +3 days) and 2nd revision (default +3 + 7 = +10 days from completion if not reviewed earlier)
            val firstReviewMillis = if (topic.nextReviewAt > 0L) {
                topic.nextReviewAt
            } else {
                baseTime + TimeUnit.DAYS.toMillis(firstIntervalDays.toLong())
            }
            scheduleSingleRevisionAlarm(
                context = context,
                alarmManager = alarmManager,
                topic = topic,
                subjectName = subjectName,
                stage = 1,
                triggerTimeMillis = normalizeToMorningHour(firstReviewMillis)
            )

            val secondReviewMillis = firstReviewMillis + TimeUnit.DAYS.toMillis(secondIntervalDays.toLong())
            scheduleSingleRevisionAlarm(
                context = context,
                alarmManager = alarmManager,
                topic = topic,
                subjectName = subjectName,
                stage = 2,
                triggerTimeMillis = normalizeToMorningHour(secondReviewMillis)
            )
        } else if (topic.reviewCount == 1) {
            // Schedule 2nd revision (+7 days from first review)
            val secondReviewMillis = if (topic.nextReviewAt > 0L) {
                topic.nextReviewAt
            } else {
                baseTime + TimeUnit.DAYS.toMillis(secondIntervalDays.toLong())
            }
            scheduleSingleRevisionAlarm(
                context = context,
                alarmManager = alarmManager,
                topic = topic,
                subjectName = subjectName,
                stage = 2,
                triggerTimeMillis = normalizeToMorningHour(secondReviewMillis)
            )
        }
    }

    private fun normalizeToMorningHour(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return if (cal.timeInMillis >= timestamp) cal.timeInMillis else timestamp
    }

    private fun scheduleSingleRevisionAlarm(
        context: Context,
        alarmManager: AlarmManager,
        topic: Topic,
        subjectName: String,
        stage: Int,
        triggerTimeMillis: Long
    ) {
        if (triggerTimeMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, ExamNotificationReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_REVISION_REMINDER)
            putExtra(EXTRA_SUBJECT_ID, topic.subjectId)
            putExtra(EXTRA_SUBJECT_NAME, subjectName)
            putExtra(EXTRA_TOPIC_ID, topic.id)
            putExtra(EXTRA_TOPIC_TITLE, topic.title)
            putExtra(EXTRA_REVIEW_STAGE, stage)
        }

        val requestCode = getRevisionRequestCode(topic.id, stage)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarmSafely(alarmManager, triggerTimeMillis, pendingIntent)
    }

    fun cancelTopicRevisionReminders(context: Context, topicId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (stage in listOf(1, 2)) {
            val intent = Intent(context, ExamNotificationReceiver::class.java)
            val requestCode = getRevisionRequestCode(topicId, stage)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun scheduleRevisionReminder(
        context: Context,
        topic: Topic,
        subjectName: String
    ) {
        scheduleTopicRevisionReminders(
            context = context,
            subjectName = subjectName,
            topic = topic
        )
    }

    fun cancelRevisionReminder(context: Context, topicId: String) {
        cancelTopicRevisionReminders(context, topicId)
    }

    private fun scheduleAlarmSafely(
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun showNotification(context: Context, subjectId: String, subjectName: String, daysLeft: Int) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SUBJECT_ID, subjectId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            subjectId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (daysLeft == 1) {
            "Tomorrow is your $subjectName exam!"
        } else {
            "$daysLeft days left for $subjectName!"
        }

        val message = "Keep up the momentum. Check off syllabus topics to stay prepared."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(subjectId.hashCode() + daysLeft, builder.build())
        } catch (e: SecurityException) {
            // Notifications permission not granted
        }
    }

    fun showRevisionNotification(
        context: Context,
        subjectId: String,
        subjectName: String,
        topicId: String,
        topicTitle: String,
        reviewStage: Int
    ) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SUBJECT_ID, subjectId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            getRevisionRequestCode(topicId, reviewStage),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cycleDay = if (reviewStage == 1) 3 else 7
        val title = "Review Topic · 5 min ($subjectName)"
        val message = "Time for your $cycleDay-day spaced repetition review of \"$topicTitle\"."

        val builder = NotificationCompat.Builder(context, REVISION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(getRevisionRequestCode(topicId, reviewStage), builder.build())
        } catch (e: SecurityException) {
            // Notifications permission not granted
        }
    }

    fun scheduleDailyStudyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        val reminderCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, ExamNotificationReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_DAILY_STUDY_REMINDER)
            putExtra(EXTRA_DAILY_HOUR, hour)
            putExtra(EXTRA_DAILY_MINUTE, minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_STUDY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmSafely(alarmManager, reminderCal.timeInMillis, pendingIntent)
    }

    fun cancelDailyStudyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ExamNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_STUDY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showDailyStudyReminderNotification(context: Context, hour: Int, minute: Int) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            DAILY_STUDY_REQUEST_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, DAILY_STUDY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Keep your study streak going")
            .setContentText("Take 30 minutes today to focus on your subjects and keep your momentum going.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(DAILY_STUDY_REQUEST_CODE, builder.build())
        } catch (e: SecurityException) {
            // Notifications permission not granted
        }

        // Automatically schedule tomorrow's daily reminder at the same hour & minute
        scheduleDailyStudyReminder(context, hour, minute)
    }

    fun getRequestCode(subjectId: String, daysBefore: Int): Int {
        return (subjectId.hashCode() and 0x0FFF) * 10 + daysBefore
    }

    fun getRevisionRequestCode(topicId: String, stage: Int): Int {
        return 100_000 + ((topicId.hashCode() and 0x7FFF) * 10) + stage
    }
}
