package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notification.NotificationHelper

class ExamNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val type = intent.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE)
            ?: NotificationHelper.TYPE_EXAM_REMINDER

        if (type == NotificationHelper.TYPE_DAILY_STUDY_REMINDER) {
            val hour = intent.getIntExtra(NotificationHelper.EXTRA_DAILY_HOUR, 20)
            val minute = intent.getIntExtra(NotificationHelper.EXTRA_DAILY_MINUTE, 0)
            NotificationHelper.showDailyStudyReminderNotification(context, hour, minute)
            return
        }

        val subjectId = intent.getStringExtra(NotificationHelper.EXTRA_SUBJECT_ID) ?: return
        val subjectName = intent.getStringExtra(NotificationHelper.EXTRA_SUBJECT_NAME) ?: "Exam"

        if (type == NotificationHelper.TYPE_REVISION_REMINDER) {
            val topicId = intent.getStringExtra(NotificationHelper.EXTRA_TOPIC_ID) ?: return
            val topicTitle = intent.getStringExtra(NotificationHelper.EXTRA_TOPIC_TITLE) ?: "Topic"
            val stage = intent.getIntExtra(NotificationHelper.EXTRA_REVIEW_STAGE, 1)
            NotificationHelper.showRevisionNotification(
                context = context,
                subjectId = subjectId,
                subjectName = subjectName,
                topicId = topicId,
                topicTitle = topicTitle,
                reviewStage = stage
            )
        } else {
            val daysLeft = intent.getIntExtra(NotificationHelper.EXTRA_DAYS_LEFT, 1)
            NotificationHelper.showNotification(context, subjectId, subjectName, daysLeft)
        }
    }
}
