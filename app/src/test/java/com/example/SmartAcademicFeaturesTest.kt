package com.example

import com.example.data.model.AttendanceSafeMarginStatus
import com.example.data.model.ClassRoutineItem
import com.example.data.model.EbbinghausIntervals
import com.example.data.model.ExamUrgencyStatus
import com.example.data.model.Subject
import com.example.data.model.SubjectWithTopics
import com.example.data.model.Topic
import com.example.data.model.calculateSmartReadiness
import com.example.util.OcrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SmartAcademicFeaturesTest {

    @Test
    fun smartExamReadiness_calculatesHighUrgencyWhenExamCloseAndProgressLow() {
        val now = System.currentTimeMillis()
        val threeDaysFromNow = now + TimeUnit.DAYS.toMillis(3) + TimeUnit.HOURS.toMillis(2)
        val subject = Subject(
            id = "sub-1",
            name = "Operating Systems",
            examDate = threeDaysFromNow,
            colorTag = "#3B82F6",
            expectedStudyHours = 12f
        )
        val topics = listOf(
            Topic(id = "t1", subjectId = "sub-1", title = "Processes", isDone = true),
            Topic(id = "t2", subjectId = "sub-1", title = "Threads", isDone = false),
            Topic(id = "t3", subjectId = "sub-1", title = "Deadlocks", isDone = false),
            Topic(id = "t4", subjectId = "sub-1", title = "Memory Management", isDone = false)
        )
        val swt = SubjectWithTopics(subject = subject, topics = topics)

        // Only 30 minutes studied out of 720 expected minutes, 25% topics done, 3 days left
        val readiness = swt.calculateSmartReadiness(
            actualFocusMinutes = 30,
            nowMillis = now
        )

        assertTrue("Score should be low (< 55)", readiness.score < 55)
        assertEquals(ExamUrgencyStatus.HIGH_URGENCY, readiness.urgencyStatus)
        assertTrue("Should recommend daily study minutes", readiness.recommendedDailyMinutes >= 60)
        assertEquals(1, readiness.completedTopics)
        assertEquals(3, readiness.remainingTopics)
    }

    @Test
    fun smartExamReadiness_calculatesReadyWhenTopicsAndStudyCompleted() {
        val now = System.currentTimeMillis()
        val sevenDaysFromNow = now + TimeUnit.DAYS.toMillis(7)
        val subject = Subject(
            id = "sub-2",
            name = "Linear Algebra",
            examDate = sevenDaysFromNow,
            colorTag = "#10B981",
            expectedStudyHours = 8f
        )
        val topics = listOf(
            Topic(id = "t1", subjectId = "sub-2", title = "Vector Spaces", isDone = true, reviewCount = 2),
            Topic(id = "t2", subjectId = "sub-2", title = "Eigenvalues", isDone = true, reviewCount = 2)
        )
        val swt = SubjectWithTopics(subject = subject, topics = topics)

        val readiness = swt.calculateSmartReadiness(
            actualFocusMinutes = 500, // > 480 expected minutes
            nowMillis = now
        )

        assertTrue("Score should be >= 85 for ready state, was ${readiness.score}", readiness.score >= 85)
        assertEquals(ExamUrgencyStatus.READY, readiness.urgencyStatus)
    }

    @Test
    fun ebbinghausIntervals_schedulesProgressiveReviewStages() {
        val baseTime = 1_700_000_000_000L
        val stage0Next = EbbinghausIntervals.computeNextReviewTimestamp(completedOrReviewedAtMillis = baseTime, revisionCount = 0)
        val stage1Next = EbbinghausIntervals.computeNextReviewTimestamp(completedOrReviewedAtMillis = baseTime, revisionCount = 1)
        val stage2Next = EbbinghausIntervals.computeNextReviewTimestamp(completedOrReviewedAtMillis = baseTime, revisionCount = 2)

        assertEquals(baseTime + TimeUnit.DAYS.toMillis(1), stage0Next)
        assertEquals(baseTime + TimeUnit.DAYS.toMillis(3), stage1Next)
        assertEquals(baseTime + TimeUnit.DAYS.toMillis(7), stage2Next)

        val dueTopic = Topic(
            id = "t-rev",
            subjectId = "sub-1",
            title = "Paging",
            isDone = true,
            reviewCount = 1,
            nextReviewAt = baseTime - TimeUnit.HOURS.toMillis(2)
        )
        assertTrue(dueTopic.isRevisionDueAt(baseTime))
    }

    @Test
    fun attendanceSafeMargin_calculatesSkippableAndRecoveryCorrectly() {
        // 18 attended, 2 missed = 20 total (90% vs 75% target)
        // Minimum classes for 18 attended at 75% is 18 / 0.75 = 24 -> can skip 4 classes safely
        val safeClass = ClassRoutineItem(
            subjectName = "Algorithms",
            dayOfWeek = "Monday",
            startTime = "09:00 AM",
            endTime = "10:00 AM",
            attendedCount = 18,
            missedCount = 2
        )
        val safeInfo = safeClass.calculateSafeMargin(targetPercentage = 75)
        assertEquals(AttendanceSafeMarginStatus.SAFE_SKIPPABLE, safeInfo.status)
        assertEquals(4, safeInfo.skippableClasses)
        assertEquals(0, safeInfo.classesNeededToRecover)

        // 6 attended, 4 missed = 10 total (60% vs 75% target)
        // Needs (0.75*10 - 6) / 0.25 = 1.5 / 0.25 = 6 consecutive classes to reach 12/16 = 75%
        val recoveryClass = ClassRoutineItem(
            subjectName = "Compiler Design",
            dayOfWeek = "Tuesday",
            startTime = "11:00 AM",
            endTime = "12:00 PM",
            attendedCount = 6,
            missedCount = 4
        )
        val recoveryInfo = recoveryClass.calculateSafeMargin(targetPercentage = 75)
        assertEquals(AttendanceSafeMarginStatus.RECOVERY_NEEDED, recoveryInfo.status)
        assertEquals(6, recoveryInfo.classesNeededToRecover)
        assertEquals(60, recoveryInfo.currentPercentage)
    }

    @Test
    fun ocrParser_extractsTimetableAndAdmitCardFieldsDeterministically() {
        val sampleTimetableText = """
            Monday 09:00 AM - 10:30 AM Data Structures Room 302 Prof. Sharma
            Wednesday 11:00 - 12:00 Operating Systems Lab-4
        """.trimIndent()

        val routines = OcrParser.parseTimetableText(sampleTimetableText)
        assertTrue("Should extract at least 2 routine rows", routines.size >= 2)
        assertEquals("MONDAY", routines[0].dayOfWeek)
        assertTrue(routines[0].subjectName.contains("Data Structures"))

        val sampleAdmitCardText = """
            UNIVERSITY END SEMESTER EXAMINATION ADMIT CARD
            Candidate Roll No: CS2026-089
            Subject: Computer Networks
            Paper Code: CS-402
            Exam Date: 2026-11-15
            Exam Center: Main Academic Block Hall B
            Room No: Room 204
            Seat No: S-42
        """.trimIndent()

        val admitCard = OcrParser.parseAdmitCardText(sampleAdmitCardText)
        assertEquals("S-42", admitCard.seatNumber)
        assertEquals("CS2026-089", admitCard.rollNumber)
        assertFalse(admitCard.roomNumber.isBlank())
    }
}
