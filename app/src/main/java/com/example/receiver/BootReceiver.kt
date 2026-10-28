package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.ExamTrackDatabase
import com.example.data.preferences.SettingsPreferences
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = SettingsPreferences(context)
                    val notificationsEnabled = prefs.notificationsEnabled.first()
                    val revisionEnabled = prefs.revisionRemindersEnabled.first()
                    if (notificationsEnabled) {
                        val db = ExamTrackDatabase.getDatabase(context)
                        val subjects = db.subjectDao().getAllSubjectsSync()
                        val subjectMap = subjects.associateBy { it.id }
                        for (subject in subjects) {
                            NotificationHelper.scheduleExamReminders(context, subject)
                        }
                        if (revisionEnabled) {
                            val topics = db.topicDao().getAllTopicsSync()
                            for (topic in topics) {
                                if (topic.isDone && topic.reviewCount < 2) {
                                    val subName = subjectMap[topic.subjectId]?.name ?: "Study"
                                    NotificationHelper.scheduleTopicRevisionReminders(
                                        context = context,
                                        subjectName = subName,
                                        topic = topic
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
