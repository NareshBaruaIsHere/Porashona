package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.CountdownStatus
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Porashona", appName)
    }

    @Test
    fun `test countdown calculations`() {
        val now = System.currentTimeMillis()

        // Today
        val todayInfo = DateUtils.getCountdownInfo(now, now)
        assertEquals(CountdownStatus.TODAY, todayInfo.status)
        assertEquals("Today!", todayInfo.displayLabel)

        // 5 days in future
        val fiveDaysLater = now + TimeUnit.DAYS.toMillis(5)
        val upcomingInfo = DateUtils.getCountdownInfo(fiveDaysLater, now)
        assertEquals(CountdownStatus.UPCOMING, upcomingInfo.status)
        assertEquals(5, upcomingInfo.daysRemaining)

        // 2 days in past
        val twoDaysPast = now - TimeUnit.DAYS.toMillis(2)
        val passedInfo = DateUtils.getCountdownInfo(twoDaysPast, now)
        assertEquals(CountdownStatus.PASSED, passedInfo.status)
        assertEquals("Exam passed", passedInfo.displayLabel)
    }

    @Test
    fun `test drawables load successfully without parsing errors`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appLogo = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_app_logo)
        org.junit.Assert.assertNotNull(appLogo)

        val githubIcon = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_github)
        org.junit.Assert.assertNotNull(githubIcon)

        val launcherBg = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
        org.junit.Assert.assertNotNull(launcherBg)

        val launcherFg = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        org.junit.Assert.assertNotNull(launcherFg)
    }
}
