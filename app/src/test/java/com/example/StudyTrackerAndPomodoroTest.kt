package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ExamTrackDatabase
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.data.repository.ExamRepository
import com.example.ui.pomodoro.PomodoroMode
import com.example.ui.pomodoro.PomodoroStatus
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudyTrackerAndPomodoroTest {

    private lateinit var database: ExamTrackDatabase
    private lateinit var repository: ExamRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ExamTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExamRepository(
            subjectDao = database.subjectDao(),
            topicDao = database.topicDao(),
            studySessionDao = database.studySessionDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testStudySessionInsertAndRetrieve() = runBlocking {
        val now = System.currentTimeMillis()
        val session1 = StudySession(
            id = "sess-1",
            subjectId = "sub-1",
            subjectName = "Algorithms",
            subjectColor = "#0D9488",
            durationMinutes = 45,
            notes = "Binary Trees and Heaps",
            sessionType = "MANUAL",
            dateMillis = now
        )

        val session2 = StudySession(
            id = "sess-2",
            subjectId = null,
            subjectName = "General Study",
            subjectColor = "#0D9488",
            durationMinutes = 30,
            notes = "Read textbook",
            sessionType = "POMODORO",
            dateMillis = now - TimeUnit.HOURS.toMillis(2)
        )

        repository.insertStudySession(session1)
        repository.insertStudySession(session2)

        val sessions = repository.allStudySessions.first()
        assertEquals(2, sessions.size)
        assertEquals(75, sessions.sumOf { it.durationMinutes })

        // Delete session1
        repository.deleteStudySessionById("sess-1")
        val remaining = repository.allStudySessions.first()
        assertEquals(1, remaining.size)
        assertEquals("sess-2", remaining[0].id)
    }

    @Test
    fun testSubjectDeletionCascadesAndCleansUp() = runBlocking {
        val subject = Subject(
            id = "sub-math",
            name = "Calculus",
            examDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7),
            colorTag = "#EF4444"
        )
        val topic1 = Topic(
            id = "top-1",
            subjectId = "sub-math",
            title = "Derivatives",
            isDone = true
        )
        val topic2 = Topic(
            id = "top-2",
            subjectId = "sub-math",
            title = "Integrals",
            isDone = false
        )

        repository.insertSubject(subject)
        repository.insertTopic(topic1)
        repository.insertTopic(topic2)

        val dataBefore = repository.getSubjectWithTopics("sub-math").first()
        assertNotNull(dataBefore)
        assertEquals(2, dataBefore?.topics?.size)

        // Delete subject
        repository.deleteSubjectById("sub-math")

        val dataAfter = repository.getSubjectWithTopics("sub-math").first()
        assertEquals(null, dataAfter)
    }

    @Test
    fun testStudyStreakCalculation() = runBlocking {
        val now = System.currentTimeMillis()
        val todayStart = DateUtils.startOfDay(now)

        // Log sessions for today, yesterday, and day before yesterday (3 days streak)
        val sToday = StudySession(
            id = "s-today",
            durationMinutes = 60,
            dateMillis = todayStart + TimeUnit.HOURS.toMillis(2)
        )
        val sYesterday = StudySession(
            id = "s-yesterday",
            durationMinutes = 45,
            dateMillis = todayStart - TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(2)
        )
        val sDayBefore = StudySession(
            id = "s-day-before",
            durationMinutes = 30,
            dateMillis = todayStart - TimeUnit.DAYS.toMillis(2) + TimeUnit.HOURS.toMillis(2)
        )

        repository.insertStudySession(sToday)
        repository.insertStudySession(sYesterday)
        repository.insertStudySession(sDayBefore)

        val sessions = repository.getAllStudySessionsSync()
        assertEquals(3, sessions.size)
    }

    @Test
    fun testPomodoroModeAttributes() {
        assertEquals(30, PomodoroMode.FOCUS.defaultMinutes)
        assertEquals(5, PomodoroMode.SHORT_BREAK.defaultMinutes)
        assertEquals(15, PomodoroMode.LONG_BREAK.defaultMinutes)
    }

    @Test
    fun testExamCountdownWithDateAndTime() {
        val localMidnight = DateUtils.startOfDay(System.currentTimeMillis())
        val tomorrowMidnight = localMidnight + TimeUnit.DAYS.toMillis(1)
        val combinedTwoPm = DateUtils.combineDateAndTime(tomorrowMidnight, "02:30 PM")
        val targetMillis = tomorrowMidnight + TimeUnit.HOURS.toMillis(14) + TimeUnit.MINUTES.toMillis(30)
        assertEquals(targetMillis, combinedTwoPm)

        val nowAtNoonToday = localMidnight + TimeUnit.HOURS.toMillis(12)
        val live = DateUtils.calculateLiveCountdown(tomorrowMidnight, nowAtNoonToday, "02:30 PM")
        assertEquals(1L, live.days)
        assertEquals(2L, live.hours)
        assertEquals(30L, live.minutes)
        assertEquals(false, live.isPassed)
    }

    @Test
    fun testRoutineWeekStartsOnSaturday() {
        val entries = com.example.ui.routine.DayOfWeek.entries
        assertEquals(com.example.ui.routine.DayOfWeek.SATURDAY, entries.first())
        assertEquals(com.example.ui.routine.DayOfWeek.FRIDAY, entries.last())
        assertEquals(7, entries.size)
    }

    @Test
    fun testLoggingOneMinuteStudySession() = runBlocking {
        val session = StudySession(
            id = "sess-1min",
            subjectId = "sub-1",
            subjectName = "Physics",
            subjectColor = "#0284C7",
            durationMinutes = 1,
            notes = "Pomodoro focus session (1 min)",
            dateMillis = System.currentTimeMillis(),
            sessionType = "POMODORO"
        )
        repository.insertStudySession(session)

        val retrieved = repository.getAllStudySessionsSync()
        assertEquals(1, retrieved.size)
        assertEquals(1, retrieved.first().durationMinutes)
        assertEquals("Physics", retrieved.first().subjectName)
    }
}
