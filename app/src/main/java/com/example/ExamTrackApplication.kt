package com.example

import android.app.Application
import com.example.data.local.ExamTrackDatabase
import com.example.data.preferences.SettingsPreferences
import com.example.data.repository.ExamRepository
import com.example.notification.NotificationHelper

class ExamTrackApplication : Application() {

    lateinit var repository: ExamRepository
        private set

    lateinit var settingsPreferences: SettingsPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        val database = ExamTrackDatabase.getDatabase(this)
        repository = ExamRepository(
            subjectDao = database.subjectDao(),
            topicDao = database.topicDao(),
            studySessionDao = database.studySessionDao(),
            classRoutineDao = database.classRoutineDao()
        )
        settingsPreferences = SettingsPreferences(this)
        NotificationHelper.createNotificationChannel(this)
    }
}
