package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.data.preferences.ThemeMode
import com.example.notification.NotificationHelper
import com.example.ui.navigation.Destinations
import com.example.ui.navigation.ExamTrackNavHost
import com.example.ui.theme.ExamTrackTheme
import com.example.widget.ExamTrackWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge-to-edge display before super.onCreate for Android 15 (SDK 35+) & backward compatibility
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val app = application as ExamTrackApplication
        val initialSubjectId = intent?.getStringExtra(NotificationHelper.EXTRA_SUBJECT_ID)
            ?: intent?.getStringExtra("extra_subject_id")

        // Ensure widget is fresh whenever app launches
        ExamTrackWidgetProvider.updateAllWidgets(this)

        setContent {
            val themeMode by app.settingsPreferences.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.DARK
            )
            val hapticFeedbackEnabled by app.settingsPreferences.hapticFeedbackEnabled.collectAsStateWithLifecycle(
                initialValue = true
            )
            val systemDark = isSystemInDarkTheme()

            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            val startupNotificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                lifecycleScope.launch {
                    app.settingsPreferences.setNotificationPermissionRequested(true)
                    if (isGranted) {
                        if (app.settingsPreferences.dailyStudyReminderEnabled.first()) {
                            val hour = app.settingsPreferences.dailyStudyReminderHour.first()
                            val minute = app.settingsPreferences.dailyStudyReminderMinute.first()
                            NotificationHelper.scheduleDailyStudyReminder(this@MainActivity, hour, minute)
                        }
                        if (app.settingsPreferences.notificationsEnabled.first()) {
                            for (subject in app.repository.getAllSubjects()) {
                                NotificationHelper.scheduleExamReminders(this@MainActivity, subject)
                            }
                        }
                    } else {
                        // Turn off reminder switches so Settings reflects that permission was not granted
                        app.settingsPreferences.setAllReminderToggles(false)
                        NotificationHelper.cancelDailyStudyReminder(this@MainActivity)
                    }
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        if (app.settingsPreferences.dailyStudyReminderEnabled.first()) {
                            val hour = app.settingsPreferences.dailyStudyReminderHour.first()
                            val minute = app.settingsPreferences.dailyStudyReminderMinute.first()
                            NotificationHelper.scheduleDailyStudyReminder(this@MainActivity, hour, minute)
                        }
                    } else {
                        val alreadyRequested = app.settingsPreferences.notificationPermissionRequested.first()
                        if (!alreadyRequested) {
                            app.settingsPreferences.setNotificationPermissionRequested(true)
                            startupNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                } else {
                    if (app.settingsPreferences.dailyStudyReminderEnabled.first()) {
                        val hour = app.settingsPreferences.dailyStudyReminderHour.first()
                        val minute = app.settingsPreferences.dailyStudyReminderMinute.first()
                        NotificationHelper.scheduleDailyStudyReminder(this@MainActivity, hour, minute)
                    }
                }
            }

            DisposableEffect(isDark) {
                val barStyle = if (isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle
                )
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
                onDispose {}
            }

            ExamTrackTheme(
                darkTheme = isDark,
                hapticFeedbackEnabled = hapticFeedbackEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(initialSubjectId) {
                        if (!initialSubjectId.isNullOrEmpty()) {
                            navController.navigate(Destinations.subjectDetail(initialSubjectId))
                        }
                    }

                    ExamTrackNavHost(
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
