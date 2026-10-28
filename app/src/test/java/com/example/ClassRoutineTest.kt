package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ExamTrackDatabase
import com.example.data.model.ClassRoutineItem
import com.example.data.repository.ExamRepository
import com.example.ui.routine.DayOfWeek
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClassRoutineTest {

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
            studySessionDao = database.studySessionDao(),
            classRoutineDao = database.classRoutineDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveClassRoutine() = runBlocking {
        val routine1 = ClassRoutineItem(
            id = "routine-1",
            subjectName = "Data Structures",
            teacherName = "Prof. Ahmed",
            roomNumber = "Room 304",
            dayOfWeek = "MONDAY",
            startTime = "10:00 AM",
            endTime = "11:15 AM",
            colorTag = "#0284C7",
            notes = "Bring notebook"
        )

        val routine2 = ClassRoutineItem(
            id = "routine-2",
            subjectName = "Physics II",
            teacherName = "Dr. Rahman",
            roomNumber = "Lab 2",
            dayOfWeek = "MONDAY",
            startTime = "12:00 PM",
            endTime = "01:15 PM",
            colorTag = "#E11D48",
            notes = "Lab coat required"
        )

        val routineTuesday = ClassRoutineItem(
            id = "routine-3",
            subjectName = "Algorithms",
            teacherName = "Prof. Khan",
            roomNumber = "Room 201",
            dayOfWeek = "TUESDAY",
            startTime = "09:00 AM",
            endTime = "10:15 AM"
        )

        repository.insertClassRoutine(routine1)
        repository.insertClassRoutine(routine2)
        repository.insertClassRoutine(routineTuesday)

        // Retrieve all
        val allRoutines = repository.allClassRoutines.first()
        assertEquals(3, allRoutines.size)

        // Retrieve for Monday
        val mondayRoutines = repository.getClassRoutinesForDay("MONDAY").first()
        assertEquals(2, mondayRoutines.size)
        assertEquals("Data Structures", mondayRoutines[0].subjectName)
        assertEquals("Prof. Ahmed", mondayRoutines[0].teacherName)
        assertEquals("Room 304", mondayRoutines[0].roomNumber)
        assertEquals("10:00 AM", mondayRoutines[0].startTime)
        assertEquals("11:15 AM", mondayRoutines[0].endTime)

        // Retrieve for Tuesday
        val tuesdayRoutines = repository.getClassRoutinesForDay("TUESDAY").first()
        assertEquals(1, tuesdayRoutines.size)
        assertEquals("Algorithms", tuesdayRoutines[0].subjectName)
    }

    @Test
    fun testUpdateClassRoutine() = runBlocking {
        val routine = ClassRoutineItem(
            id = "routine-update-1",
            subjectName = "Operating Systems",
            teacherName = "Sir Faruk",
            roomNumber = "Room 101",
            dayOfWeek = "WEDNESDAY",
            startTime = "02:00 PM",
            endTime = "03:15 PM"
        )
        repository.insertClassRoutine(routine)

        val updated = routine.copy(
            roomNumber = "Room 405",
            teacherName = "Dr. Faruk"
        )
        repository.updateClassRoutine(updated)

        val wednesday = repository.getClassRoutinesForDay("WEDNESDAY").first()
        assertEquals(1, wednesday.size)
        assertEquals("Room 405", wednesday[0].roomNumber)
        assertEquals("Dr. Faruk", wednesday[0].teacherName)
    }

    @Test
    fun testDeleteClassRoutine() = runBlocking {
        val routine = ClassRoutineItem(
            id = "routine-delete-1",
            subjectName = "Database Systems",
            teacherName = "Prof. Kabir",
            roomNumber = "Room 501",
            dayOfWeek = "THURSDAY",
            startTime = "11:00 AM",
            endTime = "12:15 PM"
        )
        repository.insertClassRoutine(routine)

        var thursday = repository.getClassRoutinesForDay("THURSDAY").first()
        assertEquals(1, thursday.size)

        repository.deleteClassRoutineById("routine-delete-1")
        thursday = repository.getClassRoutinesForDay("THURSDAY").first()
        assertEquals(0, thursday.size)
    }

    @Test
    fun testTimeParsingAndComparison() {
        assertEquals(600, DateUtils.parseTimeToMinutes("10:00 AM"))
        assertEquals(675, DateUtils.parseTimeToMinutes("11:15 AM"))
        assertEquals(720, DateUtils.parseTimeToMinutes("12:00 PM"))
        assertEquals(780, DateUtils.parseTimeToMinutes("01:00 PM"))
        assertEquals(810, DateUtils.parseTimeToMinutes("01:30 PM"))
        assertEquals(540, DateUtils.parseTimeToMinutes("09:00 AM"))

        // Time window test: 10:30 AM is 630 minutes
        assertTrue(DateUtils.isTimeBetween("10:00 AM", "11:15 AM", 630))
        assertFalse(DateUtils.isTimeBetween("10:00 AM", "11:15 AM", 590))
        assertFalse(DateUtils.isTimeBetween("10:00 AM", "11:15 AM", 700))

        assertNotNull(DateUtils.getTodayDayOfWeekString())
    }

    @Test
    fun testDayOfWeekEnum() {
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.fromKey("MONDAY"))
        assertEquals(DayOfWeek.FRIDAY, DayOfWeek.fromKey("friday"))
        assertEquals("Mon", DayOfWeek.MONDAY.shortName)
        assertEquals("Monday", DayOfWeek.MONDAY.fullName)
    }

    @Test
    fun testClockTimeSanitizationAndFormatting() {
        // Sanitizes accidental trailing characters like "10:15 AMfre"
        assertEquals("10:15 AM", DateUtils.sanitize12HourTime("10:15 AMfre"))
        assertEquals(10 to 15, DateUtils.parse24HourAndMinute("10:15 AMfre"))

        // Formats 24-hour clock selections to 12-hour AM/PM strings
        assertEquals("12:00 AM", DateUtils.format12HourTime(0, 0))
        assertEquals("09:05 AM", DateUtils.format12HourTime(9, 5))
        assertEquals("12:30 PM", DateUtils.format12HourTime(12, 30))
        assertEquals("02:45 PM", DateUtils.format12HourTime(14, 45))
    }

    @Test
    fun testAttendance80PercentThresholdAndBatchSync() = runBlocking {
        val slotSun = ClassRoutineItem(
            id = "cse201-sun",
            subjectName = "Data Structures",
            courseCode = "CSE-201",
            dayOfWeek = "SUNDAY",
            startTime = "10:00 AM",
            endTime = "11:15 AM",
            attendedCount = 9,
            missedCount = 1
        )
        val slotTue = ClassRoutineItem(
            id = "cse201-tue",
            subjectName = "Data Structures",
            courseCode = "CSE-201",
            dayOfWeek = "TUESDAY",
            startTime = "10:00 AM",
            endTime = "11:15 AM",
            attendedCount = 9,
            missedCount = 1
        )
        repository.insertClassRoutine(slotSun)
        repository.insertClassRoutine(slotTue)

        // 9/10 = 90% (>= 80% target, 1 safe skip remaining at 80%)
        val margin80 = slotSun.calculateSafeMargin(80)
        assertEquals(90, margin80.currentPercentage)
        assertEquals(80, margin80.targetPercentage)
        assertEquals(1, margin80.skippableClasses)

        // Batch update both slots when recording 1 present class
        repository.updateClassRoutines(
            listOf(
                slotSun.copy(attendedCount = 10, missedCount = 1),
                slotTue.copy(attendedCount = 10, missedCount = 1)
            )
        )
        val all = repository.getAllClassRoutinesSync()
        assertEquals(2, all.size)
        assertTrue(all.all { it.attendedCount == 10 && it.missedCount == 1 })
    }

    @Test
    fun testBackupRestoreJsonAndClearAllData() = runBlocking {
        val subject = com.example.data.model.Subject(
            id = "sub-backup-1",
            name = "Discrete Mathematics",
            examDate = System.currentTimeMillis() + 86_400_000L * 5,
            colorTag = "#0D9488",
            startTime = "09:30 AM",
            endTime = "11:30 AM",
            roomNumber = "Room 402",
            examHallBuilding = "FUB",
            seatNumber = "14B"
        )
        val topic = com.example.data.model.Topic(
            id = "top-backup-1",
            subjectId = subject.id,
            title = "Graph Theory",
            isDone = true,
            order = 0
        )
        val routine = ClassRoutineItem(
            id = "rout-backup-1",
            subjectName = "Discrete Mathematics",
            courseCode = "MAT-104",
            dayOfWeek = "SATURDAY",
            startTime = "08:30 AM",
            endTime = "10:00 AM",
            attendedCount = 12,
            missedCount = 2
        )

        repository.insertSubject(subject)
        repository.insertTopic(topic)
        repository.insertClassRoutine(routine)

        // Export JSON string
        val json = com.example.util.BackupRestoreManager.createBackupJsonString(repository)
        assertTrue(json.contains("Discrete Mathematics"))
        assertTrue(json.contains("Graph Theory"))
        assertTrue(json.contains("MAT-104"))

        // Clear all local data
        repository.clearAllData()
        assertTrue(repository.getAllSubjects().isEmpty())
        assertTrue(repository.getAllTopicsSync().isEmpty())
        assertTrue(repository.getAllClassRoutinesSync().isEmpty())

        // Restore from JSON string
        val summaryResult = com.example.util.BackupRestoreManager.restoreBackupFromJsonString(
            jsonString = json,
            repository = repository,
            clearBeforeRestore = false
        )
        assertTrue(summaryResult.isSuccess)
        val summary = summaryResult.getOrThrow()
        assertEquals(1, summary.subjectsCount)
        assertEquals(1, summary.topicsCount)
        assertEquals(1, summary.routinesCount)

        val restoredRoutines = repository.getAllClassRoutinesSync()
        assertEquals(12, restoredRoutines.first().attendedCount)
        assertEquals(2, restoredRoutines.first().missedCount)
    }

    @Test
    fun testGoogleCalendarExportIntent() {
        val subject = com.example.data.model.Subject(
            id = "sub-cal-1",
            name = "Software Engineering",
            examDate = System.currentTimeMillis() + 86_400_000L * 3,
            colorTag = "#6366F1",
            startTime = "10:00 AM",
            endTime = "12:00 PM",
            roomNumber = "512",
            examHallBuilding = "Main Block",
            seatNumber = "22"
        )
        val intent = com.example.util.CalendarExportHelper.createExamCalendarIntent(subject)
        assertEquals(android.content.Intent.ACTION_INSERT, intent.action)
        assertEquals(
            "Exam: Software Engineering",
            intent.getStringExtra(android.provider.CalendarContract.Events.TITLE)
        )
        val loc = intent.getStringExtra(android.provider.CalendarContract.Events.EVENT_LOCATION).orEmpty()
        assertTrue(loc.contains("512"))
        assertTrue(loc.contains("Main Block"))
    }
}
