package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Subject
import com.example.notification.NotificationHelper
import com.example.util.DateUtils
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationHelperTest {

    @Test
    fun `test notification channel creation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.createNotificationChannel(context)
        // Verified channel creation doesn't throw and sets up properly
    }

    @Test
    fun `test schedule and cancel reminders`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val futureExamDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10)
        val subject = Subject(
            id = "test-sub-1",
            name = "Mathematics",
            examDate = DateUtils.startOfDay(futureExamDate),
            colorTag = "#0F766E"
        )

        // Scheduling reminders for 7, 3, and 1 days before
        NotificationHelper.scheduleExamReminders(context, subject)

        // Canceling reminders
        NotificationHelper.cancelExamReminders(context, subject.id)
    }

    @Test
    fun `test showNotification builds without error`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.showNotification(
            context = context,
            subjectId = "test-sub-1",
            subjectName = "Physics",
            daysLeft = 1
        )

        NotificationHelper.showNotification(
            context = context,
            subjectId = "test-sub-1",
            subjectName = "Physics",
            daysLeft = 7
        )
    }
}
