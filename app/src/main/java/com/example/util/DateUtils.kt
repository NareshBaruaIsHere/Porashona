package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class CountdownStatus {
    PASSED,
    TODAY,
    URGENT,   // 1 to 3 days left
    UPCOMING  // 4+ days left
}

data class CountdownInfo(
    val daysRemaining: Int,
    val status: CountdownStatus,
    val displayLabel: String,
    val shortLabel: String
)

data class LiveCountdown(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val totalSecondsRemaining: Long,
    val isToday: Boolean,
    val isPassed: Boolean,
    val status: CountdownStatus,
    val formattedCompact: String
)

object DateUtils {

    private val displayFormatLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    }
    private val fullDateFormatLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    }
    private val calendarLocal = object : ThreadLocal<Calendar>() {
        override fun initialValue(): Calendar = Calendar.getInstance()
    }

    fun formatExamDate(timestamp: Long): String {
        return displayFormatLocal.get()!!.format(Date(timestamp))
    }

    fun formatFullDate(timestamp: Long): String {
        return fullDateFormatLocal.get()!!.format(Date(timestamp))
    }

    /**
     * Truncates a timestamp to 00:00:00.000 (start of day) in the current default timezone.
     */
    fun startOfDay(timestamp: Long): Long {
        val cal = calendarLocal.get()!!
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Converts a UTC midnight timestamp from Material 3 DatePicker into local start-of-day millis
     * for the exact same calendar year, month, and day.
     */
    fun utcDatePickerMillisToLocalStartOfDay(utcMillis: Long): Long {
        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Converts a local timestamp into UTC midnight millis for Material 3 DatePicker initialization.
     */
    fun localDateMillisToUtcDatePickerMillis(localMillis: Long): Long {
        val localCal = Calendar.getInstance().apply {
            timeInMillis = localMillis
        }
        return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, localCal.get(Calendar.YEAR))
            set(Calendar.MONTH, localCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Combines an exam calendar date (epoch millis) with an optional start time string (e.g., "10:00 AM" or "14:30").
     */
    fun combineDateAndTime(
        examDateMillis: Long,
        startTime: String? = null,
        defaultHour: Int = 9,
        defaultMinute: Int = 0
    ): Long {
        val rawCal = Calendar.getInstance().apply { timeInMillis = examDateMillis }
        val cleanStart = startTime
            ?.substringBefore("–")
            ?.substringBefore("-")
            ?.trim()
            .orEmpty()

        val (hour24, minute) = if (cleanStart.isNotBlank()) {
            parse24HourAndMinute(cleanStart, defaultHour = defaultHour, defaultMinute = defaultMinute)
        } else if (rawCal.get(Calendar.HOUR_OF_DAY) != 0 || rawCal.get(Calendar.MINUTE) != 0) {
            rawCal.get(Calendar.HOUR_OF_DAY) to rawCal.get(Calendar.MINUTE)
        } else {
            defaultHour to defaultMinute
        }

        return Calendar.getInstance().apply {
            timeInMillis = startOfDay(examDateMillis)
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Calculates days difference strictly by calendar days between today and the target exam date.
     */
    fun getCountdownInfo(
        examDateMillis: Long,
        currentMillis: Long = System.currentTimeMillis(),
        startTime: String? = null
    ): CountdownInfo {
        val todayStart = startOfDay(currentMillis)
        val examStart = startOfDay(examDateMillis)

        val diffMillis = examStart - todayStart
        val days = kotlin.math.round(diffMillis.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt()

        if (days == 0 && !startTime.isNullOrBlank()) {
            val targetMillis = combineDateAndTime(examDateMillis, startTime)
            if (currentMillis > targetMillis) {
                return CountdownInfo(
                    daysRemaining = -1,
                    status = CountdownStatus.PASSED,
                    displayLabel = "Exam passed",
                    shortLabel = "Passed"
                )
            }
        }

        return when {
            days < 0 -> CountdownInfo(
                daysRemaining = days,
                status = CountdownStatus.PASSED,
                displayLabel = "Exam passed",
                shortLabel = "Passed"
            )
            days == 0 -> CountdownInfo(
                daysRemaining = 0,
                status = CountdownStatus.TODAY,
                displayLabel = "Today!",
                shortLabel = "Today!"
            )
            days == 1 -> CountdownInfo(
                daysRemaining = 1,
                status = CountdownStatus.URGENT,
                displayLabel = "1 day left",
                shortLabel = "1 day left"
            )
            days in 2..3 -> CountdownInfo(
                daysRemaining = days,
                status = CountdownStatus.URGENT,
                displayLabel = "$days days left",
                shortLabel = "${days}d left"
            )
            else -> CountdownInfo(
                daysRemaining = days,
                status = CountdownStatus.UPCOMING,
                displayLabel = "$days days left",
                shortLabel = "${days}d left"
            )
        }
    }

    /**
     * Calculates high-precision real-time countdown down to the exact second using both examDateMillis and startTime.
     */
    fun calculateLiveCountdown(
        examDateMillis: Long,
        currentMillis: Long = System.currentTimeMillis(),
        startTime: String? = null
    ): LiveCountdown {
        val examStart = startOfDay(examDateMillis)
        val endOfExamDay = examStart + TimeUnit.DAYS.toMillis(1)
        val examTargetMillis = combineDateAndTime(examDateMillis, startTime, defaultHour = 9, defaultMinute = 0)

        val hasExplicitTime = !startTime.isNullOrBlank()
        val effectiveTarget = if (hasExplicitTime) {
            examTargetMillis
        } else {
            if (currentMillis in examTargetMillis until endOfExamDay) endOfExamDay else examTargetMillis
        }

        val diffToTarget = effectiveTarget - currentMillis
        val isToday = currentMillis in examStart until endOfExamDay && diffToTarget > 0
        val isPassed = diffToTarget <= 0 || currentMillis >= endOfExamDay

        if (isPassed) {
            return LiveCountdown(
                days = 0,
                hours = 0,
                minutes = 0,
                seconds = 0,
                totalSecondsRemaining = 0,
                isToday = false,
                isPassed = true,
                status = CountdownStatus.PASSED,
                formattedCompact = "Exam completed"
            )
        }

        val totalSec = diffToTarget / 1000
        val days = totalSec / (24 * 3600)
        val hours = (totalSec % (24 * 3600)) / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        val status = when {
            isToday -> CountdownStatus.TODAY
            days in 0..3 -> CountdownStatus.URGENT
            else -> CountdownStatus.UPCOMING
        }

        val formatted = if (days > 0) {
            String.format(Locale.getDefault(), "%dd %02dh %02dm %02ds", days, hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes, seconds)
        }

        return LiveCountdown(
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            totalSecondsRemaining = totalSec,
            isToday = isToday,
            isPassed = false,
            status = status,
            formattedCompact = formatted
        )
    }

    /**
     * Parses a time string into 24-hour (0..23) and minute (0..59), stripping any accidental non-time characters.
     */
    fun parse24HourAndMinute(
        timeStr: String,
        defaultHour: Int = 9,
        defaultMinute: Int = 0
    ): Pair<Int, Int> {
        if (timeStr.isBlank()) return defaultHour to defaultMinute
        val clean = timeStr.trim().uppercase(Locale.US)
        val isPm = clean.contains("PM")
        val isAm = clean.contains("AM")
        val digitsAndColon = clean.replace(Regex("[^0-9:]"), "")
        val parts = digitsAndColon.split(":").filter { it.isNotBlank() }
        if (parts.isEmpty()) return defaultHour to defaultMinute
        val rawHour = parts[0].toIntOrNull() ?: return defaultHour to defaultMinute
        val minute = (if (parts.size > 1) parts[1].take(2).toIntOrNull() ?: 0 else 0).coerceIn(0, 59)
        val normalizedHour = when {
            isPm && rawHour in 1..11 -> rawHour + 12
            isAm && rawHour == 12 -> 0
            else -> rawHour.coerceIn(0, 23)
        }
        return normalizedHour to minute
    }

    /**
     * Formats 24-hour (0..23) and minute (0..59) as a clean 12-hour AM/PM string (e.g. "09:00 AM", "01:15 PM").
     */
    fun format12HourTime(hour24: Int, minute: Int): String {
        val h = hour24.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val isPm = h >= 12
        val hour12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        val amPm = if (isPm) "PM" else "AM"
        return String.format(Locale.US, "%02d:%02d %s", hour12, m, amPm)
    }

    /**
     * Sanitizes any user/stored time string into a strict "hh:mm AM/PM" string.
     */
    fun sanitize12HourTime(
        timeStr: String,
        defaultHour: Int = 9,
        defaultMinute: Int = 0
    ): String {
        val (h, m) = parse24HourAndMinute(timeStr, defaultHour, defaultMinute)
        return format12HourTime(h, m)
    }

    /**
     * Parses time string like "10:00 AM", "01:15 PM", "14:30" into minutes from midnight (0..1439).
     * Returns -1 if parsing fails.
     */
    fun parseTimeToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return -1
        val clean = timeStr.trim().uppercase(Locale.US)
        val digitsAndColon = clean.replace(Regex("[^0-9:]"), "")
        val parts = digitsAndColon.split(":").filter { it.isNotBlank() }
        if (parts.isEmpty()) return -1
        parts[0].toIntOrNull() ?: return -1
        val (hour24, minute) = parse24HourAndMinute(timeStr)
        return (hour24 * 60 + minute).coerceIn(0, 1439)
    }

    /**
     * Checks if current minutes from midnight falls between start and end time strings.
     */
    fun isTimeBetween(startTimeStr: String, endTimeStr: String, currentMinutes: Int): Boolean {
        val start = parseTimeToMinutes(startTimeStr)
        val end = parseTimeToMinutes(endTimeStr)
        if (start < 0 || end < 0) return false
        return if (end >= start) {
            currentMinutes in start until end
        } else {
            currentMinutes >= start || currentMinutes < end
        }
    }

    fun getCurrentMinutesOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun getTodayDayOfWeekString(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            Calendar.SUNDAY -> "SUNDAY"
            else -> "MONDAY"
        }
    }
}

