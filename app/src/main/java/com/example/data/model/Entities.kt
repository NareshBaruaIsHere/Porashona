package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.DateUtils
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val examDate: Long, // Epoch millis representing exam date
    val colorTag: String, // Hex string, e.g. "#0D9488"
    val startTime: String = "", // e.g. "10:00 AM"
    val endTime: String = "",   // e.g. "01:00 PM"
    val roomNumber: String = "", // e.g. "Room 304"
    val examHallBuilding: String = "", // e.g. "Academic Building B"
    val seatNumber: String = "", // e.g. "Seat 42B" or "Row 3, Desk 14"
    val rollNumber: String = "", // e.g. "20261045"
    val admitCardNumber: String = "", // e.g. "REG-2026-9812"
    val formulaNotes: String = "", // Key formulas, theorems or quick definitions
    val driveLink: String = "", // Google Drive slides or class notes link
    val referenceLinks: String = "", // Newline-separated additional study reference links/notes
    val admitCardPhotoUri: String = "", // Attached admit card image/PDF uri
    val formulaSheetUri: String = "", // Attached formula sheet image/PDF uri
    val expectedStudyHours: Float = 6f, // Expected study hours target (default 6h)
    val createdAt: Long = System.currentTimeMillis()
) {
    val roomLocation: String
        get() = roomNumber

    val examTime: String
        get() = when {
            startTime.isNotBlank() && endTime.isNotBlank() -> "$startTime – $endTime"
            startTime.isNotBlank() -> startTime
            endTime.isNotBlank() -> endTime
            else -> ""
        }

    val notes: String
        get() = formulaNotes
}

/**
 * Ebbinghaus Spaced Repetition interval calculator.
 * Stages:
 * Stage 0 (just completed) -> Review in 1 day (or 3 days)
 * Stage 1 (reviewed once)  -> Review in 3 days
 * Stage 2 (reviewed twice) -> Review in 7 days
 * Stage 3+                 -> Review in 14 days
 */
object EbbinghausIntervals {
    private val INTERVAL_DAYS = listOf(1, 3, 7, 14)

    fun intervalDaysForStage(revisionCount: Int): Int {
        val index = revisionCount.coerceAtLeast(0).coerceAtMost(INTERVAL_DAYS.lastIndex)
        return INTERVAL_DAYS[index]
    }

    fun stageLabel(revisionCount: Int): String {
        val days = intervalDaysForStage(revisionCount)
        return "${days}-Day Cycle"
    }

    fun computeNextReviewTimestamp(
        completedOrReviewedAtMillis: Long,
        revisionCount: Int,
        examDateMillis: Long? = null
    ): Long {
        val days = intervalDaysForStage(revisionCount)
        val rawNext = completedOrReviewedAtMillis + TimeUnit.DAYS.toMillis(days.toLong())
        if (examDateMillis != null && examDateMillis > completedOrReviewedAtMillis) {
            val dayBeforeExam = examDateMillis - TimeUnit.HOURS.toMillis(18)
            if (rawNext > examDateMillis && dayBeforeExam > completedOrReviewedAtMillis + TimeUnit.HOURS.toMillis(6)) {
                return dayBeforeExam
            }
        }
        return rawNext
    }

    fun snoozeTimestamp(fromMillis: Long = System.currentTimeMillis(), hours: Int = 24): Long {
        return fromMillis + TimeUnit.HOURS.toMillis(hours.coerceAtLeast(1).toLong())
    }

    fun snoozeReviewTimestamp(fromMillis: Long = System.currentTimeMillis(), days: Int = 1): Long {
        return snoozeTimestamp(fromMillis, days.coerceAtLeast(1) * 24)
    }
}

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class Topic(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val title: String,
    val isDone: Boolean = false,
    val order: Int = 0,
    val completedAt: Long = 0L,
    val lastReviewedAt: Long = 0L,
    val reviewCount: Int = 0,
    val nextReviewAt: Long = 0L
) {
    val isCompleted: Boolean
        get() = isDone

    val name: String
        get() = title

    val revisionCount: Int
        get() = reviewCount

    val lastRevisedAt: Long
        get() = lastReviewedAt

    fun computeNextReviewMillis(
        firstIntervalDays: Int = 3,
        secondIntervalDays: Int = 7
    ): Long {
        if (!isDone) return 0L
        if (nextReviewAt > 0L) return nextReviewAt
        val baseTime = if (lastReviewedAt > 0L) lastReviewedAt else completedAt
        if (baseTime <= 0L) return 0L
        return when (reviewCount) {
            0 -> baseTime + TimeUnit.DAYS.toMillis(firstIntervalDays.toLong())
            1 -> baseTime + TimeUnit.DAYS.toMillis(secondIntervalDays.toLong())
            else -> baseTime + TimeUnit.DAYS.toMillis(14L)
        }
    }

    fun isRevisionDueAt(
        currentMillis: Long = System.currentTimeMillis(),
        firstIntervalDays: Int = 3,
        secondIntervalDays: Int = 7
    ): Boolean {
        if (!isDone) return false
        val targetMillis = computeNextReviewMillis(firstIntervalDays, secondIntervalDays)
        if (targetMillis <= 0L) return false
        return currentMillis >= targetMillis
    }

    val isRevisionDue: Boolean
        get() = isRevisionDueAt()

    fun getRevisionBadge(
        currentMillis: Long = System.currentTimeMillis(),
        firstIntervalDays: Int = 3,
        secondIntervalDays: Int = 7
    ): String? {
        if (!isDone) return null
        val targetMillis = computeNextReviewMillis(firstIntervalDays, secondIntervalDays)
        if (targetMillis <= 0L) {
            return if (reviewCount >= 3) "Mastered" else null
        }
        val diffMillis = currentMillis - targetMillis
        return if (diffMillis >= 0L) {
            val overdueDays = (diffMillis / TimeUnit.DAYS.toMillis(1)).toInt()
            val stageLabel = EbbinghausIntervals.stageLabel(reviewCount)
            if (overdueDays > 0) {
                "Review Due (${overdueDays}d overdue)"
            } else {
                "Review Due ($stageLabel)"
            }
        } else {
            val daysUntil = ceil(-diffMillis.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt()
            "Next review in ${daysUntil}d"
        }
    }

    val revisionBadge: String?
        get() = getRevisionBadge()
}

fun Topic.isDueForReview(nowMillis: Long = System.currentTimeMillis()): Boolean {
    return isRevisionDueAt(nowMillis)
}

fun Topic.isReviewOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (!isDone) return false
    val targetMillis = computeNextReviewMillis()
    if (targetMillis <= 0L) return false
    return nowMillis >= targetMillis + TimeUnit.HOURS.toMillis(24)
}

fun Topic.reviewStatusLabel(nowMillis: Long = System.currentTimeMillis()): String {
    if (!isDone) return "Incomplete"
    val targetMillis = computeNextReviewMillis()
    if (targetMillis <= 0L) return "Reviewed (${reviewCount}x)"
    val diff = nowMillis - targetMillis
    return when {
        diff >= TimeUnit.DAYS.toMillis(1) -> {
            val daysOverdue = (diff / TimeUnit.DAYS.toMillis(1)).toInt()
            "Overdue by ${daysOverdue}d"
        }
        diff >= 0L -> "Due Today"
        else -> {
            val daysUntil = ceil(-diff.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt().coerceAtLeast(1)
            "Review in ${daysUntil}d"
        }
    }
}

enum class UrgencyLevel {
    LOW,       // Ready
    MODERATE,  // On Track
    HIGH,      // Needs Attention
    CRITICAL   // High Urgency
}

enum class ExamUrgencyStatus(val label: String, val shortBadge: String) {
    READY("Ready", "Ready"),
    ON_TRACK("On Track", "On Track"),
    NEEDS_ATTENTION("Needs Attention", "Needs Attention"),
    HIGH_URGENCY("High Urgency", "High Urgency"),
    COMPLETED("Exam Passed", "Exam Passed")
}

data class ReadinessWeights(
    val topicCompletionWeight: Float = 50f,
    val studyTimeWeight: Float = 35f,
    val revisionHealthWeight: Float = 15f,
    val defaultHoursPerTopic: Float = 1.0f,
    val fallbackExpectedHoursNoTopics: Float = 4.0f
)

data class ExamReadinessResult(
    val score: Int,
    val urgencyStatus: ExamUrgencyStatus,
    val urgencyLevel: UrgencyLevel,
    val statusLabel: String,
    val badgeText: String,
    val daysRemaining: Int,
    val completedTopics: Int,
    val remainingTopics: Int,
    val totalTopics: Int,
    val loggedMinutes: Int,
    val targetMinutes: Int,
    val loggedStudyHours: Float,
    val expectedStudyHours: Float,
    val recommendedDailyMinutes: Int,
    val recommendedDailyHours: Float,
    val dueReviewCount: Int,
    val overdueReviewCount: Int,
    val recommendedAction: String,
    val explanation: String,
    val topicComponentScore: Int = 0,
    val focusComponentScore: Int = 0,
    val paceAndRevisionScore: Int = 0
) {
    val actualFocusMinutes: Int get() = loggedMinutes
    val expectedFocusMinutes: Int get() = targetMinutes
    val overdueRevisions: Int get() = dueReviewCount
    val recommendedActionText: String get() = recommendedAction
    val recommendedMessage: String get() = recommendedAction
}

typealias SmartReadinessResult = ExamReadinessResult

data class SubjectWithTopics(
    val subject: Subject,
    val topics: List<Topic> = emptyList()
) {
    val totalTopics: Int get() = topics.size
    val completedTopics: Int get() = topics.count { it.isDone }
    val remainingTopics: Int get() = (totalTopics - completedTopics).coerceAtLeast(0)
    val overdueRevisionTopics: List<Topic> get() = topics.filter { it.isRevisionDue }
    val progress: Float
        get() = if (totalTopics > 0) completedTopics.toFloat() / totalTopics.toFloat() else 0f
    val progressPercentage: Int
        get() = (progress * 100).toInt()

    val readinessPercentage: Int
        get() = if (totalTopics == 0) 100 else (progress * 100).toInt()

    val readinessStatus: String
        get() {
            return when {
                progress >= 1f -> "Exam Ready"
                progress >= 0.75f -> "Well Prepared"
                progress >= 0.40f -> "In Progress"
                else -> "Needs Urgent Focus"
            }
        }

    internal fun computeDeterministicReadinessInternal(
        loggedMinutes: Int,
        currentMillis: Long,
        weights: ReadinessWeights
    ): ExamReadinessResult = computeDeterministicReadiness(loggedMinutes, currentMillis, weights)

    private fun computeDeterministicReadiness(
        loggedMinutes: Int,
        currentMillis: Long,
        weights: ReadinessWeights
    ): ExamReadinessResult {
        val safeMinutes = loggedMinutes.coerceAtLeast(0)
        val safeHoursStudied = safeMinutes / 60f
        val rawDaysRemaining = DateUtils.getCountdownInfo(subject.examDate, currentMillis, startTime = subject.startTime).daysRemaining
        val daysRemaining = rawDaysRemaining.coerceAtLeast(0)

        val dueCount = topics.count { it.isDueForReview(currentMillis) }
        val strictOverdueCount = topics.count { it.isReviewOverdue(currentMillis) }

        val expectedTotalHours = when {
            subject.expectedStudyHours > 0f -> subject.expectedStudyHours
            totalTopics > 0 -> (totalTopics * weights.defaultHoursPerTopic).coerceAtLeast(2f)
            else -> weights.fallbackExpectedHoursNoTopics
        }
        val targetMinutes = (expectedTotalHours * 60f).roundToInt().coerceAtLeast(60)

        // Case 1: No syllabus topics defined yet
        if (totalTopics == 0) {
            val studyRatio = (safeMinutes.toFloat() / targetMinutes.toFloat()).coerceIn(0f, 1f)
            val focusPart = (studyRatio * 35f).roundToInt()
            val topicPart = if (safeMinutes > 0) 30 else 20
            val pacePart = when {
                rawDaysRemaining < 0 -> 10
                daysRemaining <= 2 && studyRatio < 0.3f -> 5
                daysRemaining <= 5 -> 12
                else -> 15
            }
            val baseScore = (topicPart + focusPart + pacePart).coerceIn(0, 100)
            val remainingMins = (targetMinutes - safeMinutes).coerceAtLeast(30)
            val effDays = if (daysRemaining <= 0) 1 else daysRemaining
            val recDailyMins = if (rawDaysRemaining < 0) 0 else (remainingMins / effDays).coerceIn(25, 240)
            val recDailyHours = recDailyMins / 60f
            val formattedDaily = String.format(Locale.US, "%.1f", recDailyHours)

            val urgencyStatus = when {
                rawDaysRemaining < 0 -> ExamUrgencyStatus.COMPLETED
                daysRemaining <= 3 && studyRatio < 0.35f -> ExamUrgencyStatus.HIGH_URGENCY
                daysRemaining <= 7 && studyRatio < 0.6f -> ExamUrgencyStatus.NEEDS_ATTENTION
                studyRatio >= 0.85f -> ExamUrgencyStatus.READY
                else -> ExamUrgencyStatus.ON_TRACK
            }
            val urgencyLevel = when (urgencyStatus) {
                ExamUrgencyStatus.HIGH_URGENCY -> UrgencyLevel.CRITICAL
                ExamUrgencyStatus.NEEDS_ATTENTION -> UrgencyLevel.HIGH
                ExamUrgencyStatus.ON_TRACK -> UrgencyLevel.MODERATE
                ExamUrgencyStatus.READY, ExamUrgencyStatus.COMPLETED -> UrgencyLevel.LOW
            }

            val dayPhrase = when {
                rawDaysRemaining < 0 -> "Exam date has passed"
                daysRemaining == 0 -> "Your exam is TODAY"
                daysRemaining == 1 -> "Your exam is in 1 day"
                else -> "Your exam is in $daysRemaining days"
            }

            val actionMsg = when (urgencyStatus) {
                ExamUrgencyStatus.COMPLETED -> "Exam date has passed."
                ExamUrgencyStatus.HIGH_URGENCY -> "Target ${recDailyMins}m of study today."
                else -> "Target ${recDailyMins}m of study today."
            }

            return ExamReadinessResult(
                score = baseScore,
                urgencyStatus = urgencyStatus,
                urgencyLevel = urgencyLevel,
                statusLabel = urgencyStatus.label,
                badgeText = urgencyStatus.shortBadge,
                daysRemaining = rawDaysRemaining,
                completedTopics = 0,
                remainingTopics = 0,
                totalTopics = 0,
                loggedMinutes = safeMinutes,
                targetMinutes = targetMinutes,
                loggedStudyHours = safeHoursStudied,
                expectedStudyHours = expectedTotalHours,
                recommendedDailyMinutes = recDailyMins,
                recommendedDailyHours = recDailyHours,
                dueReviewCount = 0,
                overdueReviewCount = 0,
                recommendedAction = actionMsg,
                explanation = "${safeMinutes}m / ${targetMinutes}m logged.",
                topicComponentScore = topicPart,
                focusComponentScore = focusPart,
                paceAndRevisionScore = pacePart
            )
        }

        // Case 2: Topics exist
        val completionRatio = completedTopics.toFloat() / totalTopics.toFloat()
        val studyRatio = (safeMinutes.toFloat() / targetMinutes.toFloat()).coerceIn(0f, 1f)
        val revisionRatio = if (completedTopics > 0) {
            ((completedTopics - dueCount).coerceAtLeast(0).toFloat() / completedTopics.toFloat())
        } else {
            1f
        }

        val topicScore = (completionRatio * 50f).roundToInt().coerceIn(0, 50)
        val studyScore = (studyRatio * 35f).roundToInt().coerceIn(0, 35)
        val revisionBase = (revisionRatio * 15f).roundToInt().coerceIn(0, 15)

        // Time pressure penalty when exam is close and workload is high
        val timePenalty = when {
            rawDaysRemaining < 0 -> 0
            remainingTopics == 0 && dueCount == 0 && studyRatio >= 0.6f -> 0
            daysRemaining == 0 && completionRatio < 0.85f -> 18
            daysRemaining in 1..3 && completionRatio < 0.45f -> 15
            daysRemaining in 1..3 && completionRatio < 0.70f -> 8
            daysRemaining in 4..7 && completionRatio < 0.40f -> 8
            else -> 0
        }

        val paceBonus = when {
            remainingTopics == 0 && dueCount == 0 -> 10
            daysRemaining >= 4 && completionRatio >= 0.75f && studyRatio >= 0.5f -> 6
            else -> 0
        }

        val paceAndRevScore = (revisionBase + paceBonus - timePenalty).coerceIn(0, 15)
        val rawScore = topicScore + studyScore + revisionBase + paceBonus - timePenalty
        val finalScore = if (remainingTopics == 0 && dueCount == 0 && studyRatio >= 0.5f) {
            rawScore.coerceIn(86, 100)
        } else {
            rawScore.coerceIn(0, 100)
        }

        // Calculate dynamic daily study target based on remaining workload & days left
        val remainingTopicMinutes = (remainingTopics * 45) + (dueCount * 15)
        val remainingTargetDeficit = (targetMinutes - safeMinutes).coerceAtLeast(0)
        val totalRemainingMinutesNeeded = maxOf(remainingTopicMinutes, (remainingTargetDeficit * 0.75f).roundToInt()) + (dueCount * 10)
        val effectiveDays = if (daysRemaining <= 0) 1 else daysRemaining

        val recDailyMins = when {
            rawDaysRemaining < 0 -> 0
            remainingTopics == 0 && dueCount == 0 && studyRatio >= 0.8f -> 25
            else -> (totalRemainingMinutesNeeded / effectiveDays).coerceIn(25, 300)
        }
        val recDailyHours = recDailyMins / 60f
        val formattedDailyHours = String.format(Locale.US, "%.1f", recDailyHours)

        val urgencyStatus = when {
            rawDaysRemaining < 0 -> ExamUrgencyStatus.COMPLETED
            finalScore >= 82 && remainingTopics == 0 && dueCount == 0 -> ExamUrgencyStatus.READY
            (daysRemaining <= 3 && (completionRatio < 0.55f || finalScore < 55)) ||
                (daysRemaining == 0 && remainingTopics > 0) ||
                finalScore < 35 -> ExamUrgencyStatus.HIGH_URGENCY
            (daysRemaining <= 7 && (completionRatio < 0.70f || finalScore < 68)) ||
                dueCount >= 2 ||
                finalScore < 55 -> ExamUrgencyStatus.NEEDS_ATTENTION
            finalScore >= 80 -> ExamUrgencyStatus.READY
            else -> ExamUrgencyStatus.ON_TRACK
        }

        val urgencyLevel = when (urgencyStatus) {
            ExamUrgencyStatus.HIGH_URGENCY -> UrgencyLevel.CRITICAL
            ExamUrgencyStatus.NEEDS_ATTENTION -> UrgencyLevel.HIGH
            ExamUrgencyStatus.ON_TRACK -> UrgencyLevel.MODERATE
            ExamUrgencyStatus.READY, ExamUrgencyStatus.COMPLETED -> UrgencyLevel.LOW
        }

        val dayPhrase = when {
            rawDaysRemaining < 0 -> "Exam has passed"
            daysRemaining == 0 -> "Your exam is TODAY"
            daysRemaining == 1 -> "Your exam is in 1 day"
            else -> "Your exam is in $daysRemaining days"
        }

        val recommendedAction = when (urgencyStatus) {
            ExamUrgencyStatus.COMPLETED -> "Exam date has passed."
            ExamUrgencyStatus.READY -> "Ready — 25m revision today."
            ExamUrgencyStatus.HIGH_URGENCY -> "Target ${recDailyMins}m of study today."
            ExamUrgencyStatus.NEEDS_ATTENTION -> "Target ${recDailyMins}m today ($remainingTopics ${if (remainingTopics == 1) "topic" else "topics"} left)."
            ExamUrgencyStatus.ON_TRACK -> "Target ${recDailyMins}m of study today."
        }

        val explanationParts = mutableListOf<String>()
        explanationParts.add("$completedTopics/$totalTopics topics done (${(completionRatio * 100).roundToInt()}%)")
        explanationParts.add("${safeMinutes / 60}h ${safeMinutes % 60}m of ${targetMinutes / 60}h target logged")
        if (dueCount > 0) {
            explanationParts.add("$dueCount spaced-repetition ${if (dueCount == 1) "review" else "reviews"} due")
        }
        val nextStep = when {
            remainingTopics > 0 && studyRatio < 0.4f ->
                "Next: Start a 25m Pomodoro focus session and complete at least 1 topic today."
            dueCount > 0 ->
                "Next: Review your $dueCount due ${if (dueCount == 1) "topic" else "topics"} to lock in long-term retention."
            remainingTopics > 0 ->
                "Next: Focus on completing the remaining $remainingTopics syllabus ${if (remainingTopics == 1) "topic" else "topics"}."
            else ->
                "Next: Review your Exam Hall Vault formula sheet and practice questions."
        }
        val explanation = explanationParts.joinToString(" • ") + ". " + nextStep

        return ExamReadinessResult(
            score = finalScore,
            urgencyStatus = urgencyStatus,
            urgencyLevel = urgencyLevel,
            statusLabel = urgencyStatus.label,
            badgeText = urgencyStatus.shortBadge,
            daysRemaining = rawDaysRemaining,
            completedTopics = completedTopics,
            remainingTopics = remainingTopics,
            totalTopics = totalTopics,
            loggedMinutes = safeMinutes,
            targetMinutes = targetMinutes,
            loggedStudyHours = safeHoursStudied,
            expectedStudyHours = expectedTotalHours,
            recommendedDailyMinutes = recDailyMins,
            recommendedDailyHours = recDailyHours,
            dueReviewCount = dueCount,
            overdueReviewCount = strictOverdueCount,
            recommendedAction = recommendedAction,
            explanation = explanation,
            topicComponentScore = topicScore,
            focusComponentScore = studyScore,
            paceAndRevisionScore = paceAndRevScore
        )
    }
}

fun SubjectWithTopics.calculateSmartReadiness(
    actualFocusMinutes: Int = 0,
    loggedStudyMinutes: Int = actualFocusMinutes,
    nowMillis: Long = System.currentTimeMillis(),
    weights: ReadinessWeights = ReadinessWeights()
): ExamReadinessResult {
    return computeDeterministicReadinessInternal(
        loggedMinutes = loggedStudyMinutes.coerceAtLeast(0),
        currentMillis = nowMillis,
        weights = weights
    )
}

fun SubjectWithTopics.calculateSmartReadiness(
    hoursStudied: Float,
    currentMillis: Long = System.currentTimeMillis(),
    weights: ReadinessWeights = ReadinessWeights()
): ExamReadinessResult {
    val mins = (hoursStudied.coerceAtLeast(0f) * 60f).roundToInt()
    return computeDeterministicReadinessInternal(
        loggedMinutes = mins,
        currentMillis = currentMillis,
        weights = weights
    )
}

@Entity(
    tableName = "study_sessions",
    indices = [Index(value = ["dateMillis"]), Index(value = ["subjectId"])]
)
data class StudySession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String? = null,
    val subjectName: String = "General Study",
    val subjectColor: String = "#0D9488",
    val durationMinutes: Int,
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val sessionType: String = "MANUAL", // "POMODORO" or "MANUAL"
    val createdAt: Long = System.currentTimeMillis()
)

enum class AttendanceStatusLevel {
    NO_CLASSES_YET,
    SAFE,
    AT_RISK_BOUNDARY,
    BELOW_THRESHOLD
}

enum class AttendanceSafeMarginStatus {
    NO_DATA,
    SAFE_SKIPPABLE,
    ON_BOUNDARY,
    RECOVERY_NEEDED;

    companion object {
        val AT_BOUNDARY = ON_BOUNDARY
        val RECOVERY_REQUIRED = RECOVERY_NEEDED
    }
}

typealias AttendanceMarginStatus = AttendanceSafeMarginStatus

data class AttendanceSafeMarginInfo(
    val attendedClasses: Int,
    val missedClasses: Int,
    val totalClasses: Int,
    val currentPercentage: Int,
    val targetPercentage: Int,
    val skippableClasses: Int,
    val classesNeededToRecover: Int,
    val status: AttendanceSafeMarginStatus,
    val headlineText: String,
    val detailText: String
) {
    val attended: Int get() = attendedClasses
    val missed: Int get() = missedClasses
    val totalConducted: Int get() = totalClasses
    val isBelowThreshold: Boolean get() = status == AttendanceSafeMarginStatus.RECOVERY_NEEDED
    val headline: String get() = headlineText
    val explanation: String get() = detailText
}

data class AttendanceSummary(
    val totalClasses: Int,
    val attendedClasses: Int,
    val missedClasses: Int,
    val currentPercentage: Int,
    val exactPercentage: Float,
    val requiredPercentage: Int,
    val safeSkipsRemaining: Int,
    val consecutiveClassesToRecover: Int,
    val statusLevel: AttendanceStatusLevel,
    val statusMessage: String
)

@Entity(
    tableName = "class_routines",
    indices = [Index(value = ["dayOfWeek"]), Index(value = ["startTime"])]
)
data class ClassRoutineItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val subjectName: String,
    val courseCode: String = "",  // e.g. "CSE-201"
    val teacherName: String = "", // e.g. "Prof. Ahmed" or "Sir Name"
    val roomNumber: String = "",  // e.g. "Room 304", "Lab 2"
    val dayOfWeek: String,        // "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    val startTime: String,        // e.g. "10:00 AM"
    val endTime: String,          // e.g. "11:15 AM"
    val colorTag: String = "#0284C7",
    val notes: String = "",
    val attendedCount: Int = 0,
    val missedCount: Int = 0,
    val lastAttendanceDate: Long = 0L,
    val lastAttendanceStatus: Int = 0, // 0 = None, 1 = Attended, 2 = Missed
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalConducted: Int get() = attendedCount + missedCount
    val attendancePercentage: Int
        get() = if (totalConducted > 0) ((attendedCount.toDouble() / totalConducted.toDouble()) * 100.0).toInt() else 100

    fun calculateAttendance(requiredPercentage: Int = 75): AttendanceSummary {
        val safeThreshold = requiredPercentage.coerceIn(1, 99)
        val p = safeThreshold / 100.0
        val t = totalConducted
        val a = attendedCount
        val m = missedCount

        if (t == 0) {
            return AttendanceSummary(
                totalClasses = 0,
                attendedClasses = 0,
                missedClasses = 0,
                currentPercentage = 100,
                exactPercentage = 100f,
                requiredPercentage = safeThreshold,
                safeSkipsRemaining = 0,
                consecutiveClassesToRecover = 0,
                statusLevel = AttendanceStatusLevel.NO_CLASSES_YET,
                statusMessage = "100% Attendance — No classes recorded yet (Target: $safeThreshold%)."
            )
        }

        val exactPct = (a.toFloat() / t.toFloat()) * 100f
        val currentPctInt = exactPct.toInt()
        val isMeetingThreshold = (a.toDouble() / t.toDouble()) + 1e-9 >= p

        return if (isMeetingThreshold) {
            val rawSafeSkips = floor((a.toDouble() / p) - t.toDouble() + 1e-9).toInt().coerceAtLeast(0)
            if (rawSafeSkips > 0) {
                AttendanceSummary(
                    totalClasses = t,
                    attendedClasses = a,
                    missedClasses = m,
                    currentPercentage = currentPctInt,
                    exactPercentage = exactPct,
                    requiredPercentage = safeThreshold,
                    safeSkipsRemaining = rawSafeSkips,
                    consecutiveClassesToRecover = 0,
                    statusLevel = AttendanceStatusLevel.SAFE,
                    statusMessage = "$currentPctInt% Attendance — You can miss $rawSafeSkips more ${if (rawSafeSkips == 1) "class" else "classes"} while maintaining $safeThreshold% attendance."
                )
            } else {
                AttendanceSummary(
                    totalClasses = t,
                    attendedClasses = a,
                    missedClasses = m,
                    currentPercentage = currentPctInt,
                    exactPercentage = exactPct,
                    requiredPercentage = safeThreshold,
                    safeSkipsRemaining = 0,
                    consecutiveClassesToRecover = 0,
                    statusLevel = AttendanceStatusLevel.AT_RISK_BOUNDARY,
                    statusMessage = "$currentPctInt% Attendance — At risk! Missing the next class will drop you below $safeThreshold%."
                )
            }
        } else {
            val deficit = p * t.toDouble() - a.toDouble()
            val needed = if (deficit > 0) ceil((deficit / (1.0 - p)) - 1e-9).toInt().coerceAtLeast(1) else 1
            AttendanceSummary(
                totalClasses = t,
                attendedClasses = a,
                missedClasses = m,
                currentPercentage = currentPctInt,
                exactPercentage = exactPct,
                requiredPercentage = safeThreshold,
                safeSkipsRemaining = 0,
                consecutiveClassesToRecover = needed,
                statusLevel = AttendanceStatusLevel.BELOW_THRESHOLD,
                statusMessage = "$currentPctInt% Attendance — Below the required $safeThreshold%. Attend the next $needed consecutive ${if (needed == 1) "class" else "classes"} to recover."
            )
        }
    }

    fun calculateSafeMargin(targetPercentage: Int = 75): AttendanceSafeMarginInfo {
        val summary = calculateAttendance(targetPercentage)
        val status = when (summary.statusLevel) {
            AttendanceStatusLevel.NO_CLASSES_YET -> AttendanceSafeMarginStatus.NO_DATA
            AttendanceStatusLevel.SAFE -> AttendanceSafeMarginStatus.SAFE_SKIPPABLE
            AttendanceStatusLevel.AT_RISK_BOUNDARY -> AttendanceSafeMarginStatus.ON_BOUNDARY
            AttendanceStatusLevel.BELOW_THRESHOLD -> AttendanceSafeMarginStatus.RECOVERY_NEEDED
        }
        val headline = when (status) {
            AttendanceSafeMarginStatus.NO_DATA -> "No classes logged yet (Target: ${summary.requiredPercentage}%)"
            AttendanceSafeMarginStatus.SAFE_SKIPPABLE -> "${summary.currentPercentage}% Attendance — Can miss ${summary.safeSkipsRemaining} more ${if (summary.safeSkipsRemaining == 1) "class" else "classes"}"
            AttendanceSafeMarginStatus.ON_BOUNDARY -> "${summary.currentPercentage}% Attendance — On the ${summary.requiredPercentage}% boundary"
            AttendanceSafeMarginStatus.RECOVERY_NEEDED -> "${summary.currentPercentage}% Attendance — Below ${summary.requiredPercentage}%"
        }
        val detail = when (status) {
            AttendanceSafeMarginStatus.NO_DATA -> "Mark classes as Attended or Missed to calculate your safe skip margin."
            AttendanceSafeMarginStatus.SAFE_SKIPPABLE -> "You can miss ${summary.safeSkipsRemaining} more ${if (summary.safeSkipsRemaining == 1) "class" else "classes"} while maintaining ${summary.requiredPercentage}% attendance (${summary.attendedClasses}/${summary.totalClasses} attended)."
            AttendanceSafeMarginStatus.ON_BOUNDARY -> "Do not miss the next lecture — skipping 1 class will drop you below ${summary.requiredPercentage}% (${summary.attendedClasses}/${summary.totalClasses} attended)."
            AttendanceSafeMarginStatus.RECOVERY_NEEDED -> "Attend the next ${summary.consecutiveClassesToRecover} consecutive ${if (summary.consecutiveClassesToRecover == 1) "class" else "classes"} to recover to ${summary.requiredPercentage}% (${summary.attendedClasses}/${summary.totalClasses} attended)."
        }
        return AttendanceSafeMarginInfo(
            attendedClasses = summary.attendedClasses,
            missedClasses = summary.missedClasses,
            totalClasses = summary.totalClasses,
            currentPercentage = summary.currentPercentage,
            targetPercentage = summary.requiredPercentage,
            skippableClasses = summary.safeSkipsRemaining,
            classesNeededToRecover = summary.consecutiveClassesToRecover,
            status = status,
            headlineText = headline,
            detailText = detail
        )
    }

    val isAttendanceWarning: Boolean
        get() = totalConducted > 0 && calculateAttendance(75).statusLevel == AttendanceStatusLevel.BELOW_THRESHOLD

    val safeSkips: Int
        get() = calculateAttendance(75).safeSkipsRemaining

    val classesNeededFor75: Int
        get() = calculateAttendance(75).consecutiveClassesToRecover

    val safeMarginSummary: String
        get() {
            val summary = calculateAttendance(75)
            return when (summary.statusLevel) {
                AttendanceStatusLevel.NO_CLASSES_YET -> "100% Attendance — New Semester"
                AttendanceStatusLevel.SAFE -> summary.statusMessage
                AttendanceStatusLevel.AT_RISK_BOUNDARY -> summary.statusMessage
                AttendanceStatusLevel.BELOW_THRESHOLD -> summary.statusMessage
            }
        }
}
