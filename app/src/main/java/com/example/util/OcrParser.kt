package com.example.util

import com.example.data.model.ClassRoutineItem
import com.example.data.model.Subject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ScannedRoutineCandidate(
    val id: String = UUID.randomUUID().toString(),
    val subjectName: String,
    val courseCode: String = "",
    val teacherName: String = "",
    val dayOfWeek: String = "MONDAY",
    val startTime: String = "09:00 AM",
    val endTime: String = "10:15 AM",
    val roomNumber: String = "",
    val colorTag: String = "#0284C7",
    val notes: String = "",
    val isUncertain: Boolean = false,
    val isDuplicate: Boolean = false,
    val warningMessage: String? = null,
    val isSelected: Boolean = true
) {
    val isValid: Boolean
        get() = subjectName.trim().isNotBlank()

    fun toClassRoutineItem(): ClassRoutineItem {
        val cleaned = OcrParser.sanitizeRoutineCandidate(this)
        return ClassRoutineItem(
            subjectName = cleaned.subjectName,
            courseCode = cleaned.courseCode,
            teacherName = cleaned.teacherName,
            dayOfWeek = cleaned.dayOfWeek.uppercase(Locale.US),
            startTime = cleaned.startTime.ifBlank { "09:00 AM" },
            endTime = cleaned.endTime.ifBlank { "10:15 AM" },
            roomNumber = cleaned.roomNumber,
            colorTag = colorTag,
            notes = cleaned.notes
        )
    }
}

data class ScannedAdmitCardCandidate(
    val id: String = UUID.randomUUID().toString(),
    val subjectName: String = "",
    val courseCode: String = "",
    val examDateMillis: Long? = null,
    val examDateText: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val examHallBuilding: String = "",
    val roomNumber: String = "",
    val seatNumber: String = "",
    val rollNumber: String = "",
    val registrationNumber: String = "",
    val colorTag: String = "#4F46E5",
    val rawText: String = "",
    val uncertainFields: List<String> = emptyList(),
    val isSelected: Boolean = true
) {
    val isValid: Boolean
        get() = OcrParser.cleanSubjectField(subjectName).isNotBlank() ||
            OcrParser.cleanCourseCodeField(courseCode).isNotBlank()

    fun sanitized(): ScannedAdmitCardCandidate = OcrParser.sanitizeAdmitCardCandidate(this)

    fun toSubject(attachedUri: String = ""): Subject {
        val cleaned = sanitized()
        val displayName = when {
            cleaned.subjectName.isNotBlank() &&
                cleaned.courseCode.isNotBlank() &&
                !cleaned.subjectName.contains(cleaned.courseCode, ignoreCase = true) ->
                "${cleaned.courseCode} ${cleaned.subjectName}".trim()
            cleaned.subjectName.isNotBlank() -> cleaned.subjectName
            cleaned.courseCode.isNotBlank() -> cleaned.courseCode
            else -> "Scanned Exam"
        }
        val dateMillis = cleaned.examDateMillis
            ?: (DateUtils.startOfDay(System.currentTimeMillis()) + TimeUnit.DAYS.toMillis(7))
        return Subject(
            name = displayName,
            examDate = dateMillis,
            colorTag = colorTag,
            startTime = cleaned.startTime.ifBlank { "10:00 AM" },
            endTime = cleaned.endTime.ifBlank { "01:00 PM" },
            roomNumber = cleaned.roomNumber,
            examHallBuilding = cleaned.examHallBuilding,
            seatNumber = cleaned.seatNumber,
            rollNumber = cleaned.rollNumber,
            admitCardNumber = cleaned.registrationNumber,
            admitCardPhotoUri = attachedUri
        )
    }
}

typealias ScannedExamCandidate = ScannedAdmitCardCandidate

object OcrParser {

    // Regex-based OCR Text Cleaning Patterns
    private val CONTROL_AND_ZERO_WIDTH_REGEX = Regex("[\\p{Cntrl}&&[^\r\n\t]]|[\\u200B-\\u200D\\uFEFF]")
    private val HORIZONTAL_WHITESPACE_REGEX = Regex("[ \\t\\u00A0]+")
    private val NON_ESSENTIAL_WHITESPACE_REGEX = Regex("\\s+")
    private val OCR_NOISE_SYMBOLS_REGEX = Regex("[|_~^=*#•·«»„“”‘’`´]+")
    private val LEADING_LIST_MARKER_REGEX = Regex("^(?:\\(?\\d{1,2}[.)]|\\([a-zA-Z]\\)|[-•*]+)\\s*")
    private val SUBJECT_LABEL_PREFIX_REGEX = Regex(
        "^(?:Subject(?:\\s*Name|\\s*Title|\\s*Code)?|Course(?:\\s*Title|\\s*Name|\\s*Code)?|Paper(?:\\s*Name|\\s*Title|\\s*Code)?|Exam(?:\\s*Name|\\s*Subject)?|Title|Name)\\s*[:#=.\\-]*\\s*",
        RegexOption.IGNORE_CASE
    )
    private val ROOM_LABEL_PREFIX_REGEX = Regex(
        "^(?:Room(?:\\s*No|\\s*Number)?|Rm|Hall(?:\\s*No)?|Venue)\\s*[:#=.\\-]*\\s*",
        RegexOption.IGNORE_CASE
    )
    private val BUILDING_LABEL_PREFIX_REGEX = Regex(
        "^(?:Building|Center|Centre|Campus|Venue|Exam\\s*Hall)\\s*[:#=.\\-]*\\s*",
        RegexOption.IGNORE_CASE
    )
    private val STRIP_SUBJECT_PUNCTUATION_REGEX = Regex("[^\\p{L}\\p{N}\\s&]")
    private val STRIP_ROOM_PUNCTUATION_REGEX = Regex("[^\\p{L}\\p{N}\\s-]")
    private val STRIP_BUILDING_PUNCTUATION_REGEX = Regex("[^\\p{L}\\p{N}\\s&-]")
    private val STRIP_CODE_PUNCTUATION_REGEX = Regex("[^A-Za-z0-9-]")
    private val STRIP_IDENTIFIER_PUNCTUATION_REGEX = Regex("[^A-Za-z0-9-]")
    private val STRIP_TEACHER_PUNCTUATION_REGEX = Regex("[^\\p{L}\\s]")
    private val MULTIPLE_HYPHENS_REGEX = Regex("-{2,}")
    private val EDGE_PUNCTUATION_OR_SPACE_REGEX = Regex("^[\\s\\p{Punct}]+|[\\s\\p{Punct}]+$")

    /**
     * Cleans raw OCR line text by stripping control characters, OCR table borders,
     * and collapsing non-essential horizontal whitespace.
     */
    fun cleanOcrRawLine(rawLine: String): String {
        return rawLine
            .replace(CONTROL_AND_ZERO_WIDTH_REGEX, "")
            .replace(HORIZONTAL_WHITESPACE_REGEX, " ")
            .trim()
    }

    /**
     * Strips punctuation and non-essential whitespace from a scanned Subject Name
     * before populating the Add Subject field.
     */
    fun cleanSubjectField(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(CONTROL_AND_ZERO_WIDTH_REGEX, " ")
            .replace(LEADING_LIST_MARKER_REGEX, "")
            .replace(SUBJECT_LABEL_PREFIX_REGEX, "")
            .replace(Regex("\\b\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}\\b"), " ")
            .replace(STRIP_SUBJECT_PUNCTUATION_REGEX, " ")
            .replace(NON_ESSENTIAL_WHITESPACE_REGEX, " ")
            .replace(EDGE_PUNCTUATION_OR_SPACE_REGEX, "")
            .trim()
    }

    /**
     * Strips punctuation and non-essential whitespace from a scanned Course Code.
     */
    fun cleanCourseCodeField(raw: String): String {
        if (raw.isBlank()) return ""
        val normalized = raw
            .replace(CONTROL_AND_ZERO_WIDTH_REGEX, "")
            .trim()
            .replace(Regex("^([A-Za-z]{2,4})\\s+(\\d{3,4}[A-Za-z]?)$"), "$1-$2")
            .replace(STRIP_CODE_PUNCTUATION_REGEX, "")
            .replace(MULTIPLE_HYPHENS_REGEX, "-")
            .replace(EDGE_PUNCTUATION_OR_SPACE_REGEX, "")
            .uppercase(Locale.US)
        return normalized
    }

    /**
     * Strips punctuation and non-essential whitespace from a scanned Room Number field.
     */
    fun cleanRoomField(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(CONTROL_AND_ZERO_WIDTH_REGEX, " ")
            .replace(ROOM_LABEL_PREFIX_REGEX, "")
            .replace(STRIP_ROOM_PUNCTUATION_REGEX, " ")
            .replace(MULTIPLE_HYPHENS_REGEX, "-")
            .replace(NON_ESSENTIAL_WHITESPACE_REGEX, " ")
            .replace(EDGE_PUNCTUATION_OR_SPACE_REGEX, "")
            .trim()
    }

    /**
     * Strips punctuation and non-essential whitespace from a scanned Building / Exam Hall field.
     */
    fun cleanBuildingField(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(CONTROL_AND_ZERO_WIDTH_REGEX, " ")
            .replace(BUILDING_LABEL_PREFIX_REGEX, "")
            .replace(STRIP_BUILDING_PUNCTUATION_REGEX, " ")
            .replace(MULTIPLE_HYPHENS_REGEX, "-")
            .replace(NON_ESSENTIAL_WHITESPACE_REGEX, " ")
            .replace(EDGE_PUNCTUATION_OR_SPACE_REGEX, "")
            .trim()
    }

    /**
     * Strips punctuation and non-essential whitespace from Roll, Seat, or Registration numbers.
     */
    fun cleanIdentifierField(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(CONTROL_AND_ZERO_WIDTH_REGEX, "")
            .replace(STRIP_IDENTIFIER_PUNCTUATION_REGEX, "")
            .replace(MULTIPLE_HYPHENS_REGEX, "-")
            .replace(EDGE_PUNCTUATION_OR_SPACE_REGEX, "")
            .trim()
    }

    /**
     * Strips punctuation and non-essential whitespace from teacher/professor names.
     */
    fun cleanTeacherField(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(CONTROL_AND_ZERO_WIDTH_REGEX, " ")
            .replace(STRIP_TEACHER_PUNCTUATION_REGEX, " ")
            .replace(NON_ESSENTIAL_WHITESPACE_REGEX, " ")
            .trim()
    }

    /**
     * Strips noisy OCR symbols and collapses non-essential whitespace in notes.
     */
    fun cleanNotesField(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(CONTROL_AND_ZERO_WIDTH_REGEX, " ")
            .replace(OCR_NOISE_SYMBOLS_REGEX, " ")
            .replace(NON_ESSENTIAL_WHITESPACE_REGEX, " ")
            .replace(EDGE_PUNCTUATION_OR_SPACE_REGEX, "")
            .trim()
    }

    /**
     * Applies the regex-based cleaning layer across all fields of a ScannedAdmitCardCandidate
     * before populating the Add Subject screen or saving to the database.
     */
    fun sanitizeAdmitCardCandidate(candidate: ScannedAdmitCardCandidate): ScannedAdmitCardCandidate {
        val cleanedSubject = cleanSubjectField(candidate.subjectName)
        val cleanedCode = cleanCourseCodeField(candidate.courseCode)
        val cleanedStart = normalizeTimeString(candidate.startTime, "AM").ifBlank {
            candidate.startTime.replace(NON_ESSENTIAL_WHITESPACE_REGEX, " ").trim()
        }
        val cleanedEnd = normalizeTimeString(candidate.endTime, "PM", afterStart = cleanedStart.ifBlank { null }).ifBlank {
            candidate.endTime.replace(NON_ESSENTIAL_WHITESPACE_REGEX, " ").trim()
        }
        return candidate.copy(
            subjectName = cleanedSubject,
            courseCode = cleanedCode,
            examDateText = candidate.examDateText.replace(NON_ESSENTIAL_WHITESPACE_REGEX, " ").trim(),
            startTime = cleanedStart,
            endTime = cleanedEnd,
            examHallBuilding = cleanBuildingField(candidate.examHallBuilding),
            roomNumber = cleanRoomField(candidate.roomNumber),
            seatNumber = cleanIdentifierField(candidate.seatNumber),
            rollNumber = cleanIdentifierField(candidate.rollNumber),
            registrationNumber = cleanIdentifierField(candidate.registrationNumber)
        )
    }

    /**
     * Applies the regex-based cleaning layer across all fields of a ScannedRoutineCandidate.
     */
    fun sanitizeRoutineCandidate(candidate: ScannedRoutineCandidate): ScannedRoutineCandidate {
        val cleanedStart = normalizeTimeString(candidate.startTime, "AM").ifBlank { "09:00 AM" }
        val cleanedEnd = normalizeTimeString(candidate.endTime, "AM", afterStart = cleanedStart).ifBlank { "10:15 AM" }
        return candidate.copy(
            subjectName = cleanSubjectField(candidate.subjectName),
            courseCode = cleanCourseCodeField(candidate.courseCode),
            teacherName = cleanTeacherField(candidate.teacherName),
            startTime = cleanedStart,
            endTime = cleanedEnd,
            roomNumber = cleanRoomField(candidate.roomNumber),
            notes = cleanNotesField(candidate.notes)
        )
    }

    private val DAY_PATTERNS = listOf(
        "MONDAY" to Regex("\\b(monday|mon)\\b", RegexOption.IGNORE_CASE),
        "TUESDAY" to Regex("\\b(tuesday|tue|tues)\\b", RegexOption.IGNORE_CASE),
        "WEDNESDAY" to Regex("\\b(wednesday|wed)\\b", RegexOption.IGNORE_CASE),
        "THURSDAY" to Regex("\\b(thursday|thu|thur|thurs)\\b", RegexOption.IGNORE_CASE),
        "FRIDAY" to Regex("\\b(friday|fri)\\b", RegexOption.IGNORE_CASE),
        "SATURDAY" to Regex("\\b(saturday|sat)\\b", RegexOption.IGNORE_CASE),
        "SUNDAY" to Regex("\\b(sunday|sun)\\b", RegexOption.IGNORE_CASE)
    )

    private val TIME_RANGE_REGEX = Regex(
        "\\b(\\d{1,2}[:.]\\d{2}\\s*(?:AM|PM|am|pm)?|\\d{1,2}\\s*(?:AM|PM|am|pm))\\s*(?:[-–—~]+|\\bto\\b)\\s*(\\d{1,2}[:.]\\d{2}\\s*(?:AM|PM|am|pm)?|\\d{1,2}\\s*(?:AM|PM|am|pm))\\b",
        RegexOption.IGNORE_CASE
    )

    private val SINGLE_TIME_REGEX = Regex(
        "\\b(\\d{1,2}[:.]\\d{2}\\s*(?:AM|PM|am|pm)|\\d{1,2}\\s*(?:AM|PM|am|pm)|(?:[01]?\\d|2[0-3]):[0-5]\\d)\\b",
        RegexOption.IGNORE_CASE
    )

    private val COURSE_CODE_REGEX = Regex(
        "\\b([A-Z]{2,4}\\s*[-]?\\s*\\d{3,4}[A-Z]?)\\b"
    )

    private val ROOM_REGEX = Regex(
        "\\b(?:Room|Rm|Lab|Hall|Gallery|Auditorium|Block|CR|LH)\\s*[:#.-]?\\s*([A-Za-z0-9-]+(?:\\s+[A-Za-z0-9-]+)?)",
        RegexOption.IGNORE_CASE
    )

    private val TEACHER_REGEX = Regex(
        "\\b(?:Prof\\.?|Professor|Dr\\.?|Sir|Madam|Ms\\.?|Mr\\.?|Lecturer|Instructor|Faculty)\\s*[:.-]?\\s*([A-Z][a-zA-Z.\\s]{2,28})"
    )

    private val ROLL_REGEX = Regex(
        "(?:Roll\\s*(?:No|Number|#)?|Candidate\\s*ID|Student\\s*ID)\\s*[:#=-]?\\s*([A-Z0-9-]{3,20})",
        RegexOption.IGNORE_CASE
    )

    private val REG_REGEX = Regex(
        "(?:Reg(?:istration)?\\s*(?:No|Number|#)?|Admit\\s*(?:Card)?\\s*(?:No|ID)?)\\s*[:#=-]?\\s*([A-Z0-9-]{3,22})",
        RegexOption.IGNORE_CASE
    )

    private val SEAT_REGEX = Regex(
        "(?:Seat\\s*(?:No|Number|Allotment)?|Desk|Bench)\\s*[:#=-]?\\s*([A-Za-z0-9-]{1,12})",
        RegexOption.IGNORE_CASE
    )

    private val BUILDING_REGEX = Regex(
        "(?:Building|Center|Centre|Campus|Venue|Exam\\s*Hall)\\s*[:#=-]?\\s*([A-Za-z0-9\\s,.-]{3,35})",
        RegexOption.IGNORE_CASE
    )

    private val DATE_REGEX_LIST = listOf(
        Regex("\\b(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{4})\\b"),
        Regex("\\b(\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2})\\b"),
        Regex("\\b(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2})\\b"),
        Regex("\\b((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*\\.?\\s+\\d{1,2}(?:st|nd|rd|th)?,?\\s+\\d{2,4})\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(\\d{1,2}(?:st|nd|rd|th)?\\s+(?:of\\s+)?(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*\\.?,?\\s+\\d{2,4})\\b", RegexOption.IGNORE_CASE)
    )

    private val BOILERPLATE_KEYWORDS = listOf(
        "admit card",
        "hall ticket",
        "university",
        "institute of",
        "college of",
        "examination",
        "semester",
        "controller of",
        "registrar",
        "instructions",
        "candidate",
        "signature",
        "photograph",
        "date of birth",
        "father",
        "mother",
        "department of",
        "faculty of",
        "schedule of",
        "time table",
        "timetable",
        "routine",
        "sl no",
        "s.no",
        "page "
    )

    private val BARE_ROOM_TOKEN_REGEX = Regex("^(?:[A-Za-z]?\\d{3,4}[A-Za-z]?|[A-Za-z]-?\\d{2,4}|Lab\\s*\\d+|Room\\s*\\w+)$", RegexOption.IGNORE_CASE)
    private val PARENTHETICAL_NOTE_REGEX = Regex("\\([^)]*\\)")
    private val TABLE_HEADER_KEYWORDS = setOf("day", "days", "time", "timing", "subject", "course", "teacher", "faculty", "instructor", "room", "venue", "hall", "period")

    fun normalizeDayKey(raw: String, defaultDay: String = "MONDAY"): String {
        val detected = detectDayOfWeek(raw)
        if (detected != null) return detected
        val upper = raw.trim().uppercase(Locale.US)
        val validDays = setOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        return if (upper in validDays) upper else defaultDay.uppercase(Locale.US)
    }

    fun parseDateStringToMillis(rawDate: String): Long? {
        if (rawDate.isBlank()) return null
        return tryParseDateMillis(rawDate) ?: findDateInText(rawDate)?.second
    }

    private fun isTableHeaderRow(line: String): Boolean {
        if (TIME_RANGE_REGEX.containsMatchIn(line) || SINGLE_TIME_REGEX.containsMatchIn(line)) return false
        val tokens = line.lowercase(Locale.US)
            .split(Regex("[|\\s,/:-]+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        val headerMatches = tokens.count { it in TABLE_HEADER_KEYWORDS }
        return headerMatches >= 2 && headerMatches.toFloat() / tokens.size.toFloat() >= 0.5f
    }

    /**
     * Parses raw OCR text into a list of candidate timetable entries, checking for duplicates
     * against existing routines and marking incomplete/uncertain items for user review.
     * Supports multi-column timetable grids (Day | Time | Subject | Teacher | Room),
     * 2-letter course abbreviations (AI, DC, SE, CN), bare room numbers (608, 405, 1001),
     * faculty initials (THA, FK, KD, TASMIN), and merged Day cells.
     */
    fun parseTimetableText(
        rawText: String,
        existingRoutines: List<ClassRoutineItem> = emptyList(),
        defaultDay: String = "MONDAY"
    ): List<ScannedRoutineCandidate> {
        if (rawText.isBlank()) return emptyList()

        val lines = rawText.lines()
            .map { cleanOcrRawLine(it) }
            .filter { it.isNotBlank() && !isTableHeaderRow(it) }

        val results = mutableListOf<ScannedRoutineCandidate>()
        var currentDayContext = defaultDay.uppercase(Locale.US)

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            val detectedDay = detectDayOfWeek(line)
            if (detectedDay != null) {
                currentDayContext = detectedDay
                val strippedWithoutNotes = line.replace(PARENTHETICAL_NOTE_REGEX, " ")
                val strippedAlpha = strippedWithoutNotes.replace(Regex("[^A-Za-z]"), "").uppercase(Locale.US)
                if (strippedAlpha == detectedDay ||
                    (strippedAlpha.length <= 9 &&
                        TIME_RANGE_REGEX.find(line) == null &&
                        SINGLE_TIME_REGEX.find(line) == null &&
                        COURSE_CODE_REGEX.find(line) == null)
                ) {
                    i++
                    continue
                }
            }

            val timeMatch = TIME_RANGE_REGEX.find(line)
            val singleTimeMatch = if (timeMatch == null) SINGLE_TIME_REGEX.find(line) else null
            val codeMatch = COURSE_CODE_REGEX.find(line)

            if (timeMatch != null || singleTimeMatch != null || codeMatch != null) {
                val windowLines = mutableListOf(line)
                // Only merge the next line if the current line has no letters outside the day/time tokens
                val lineWithoutTimeAndDay = line
                    .let { l -> timeMatch?.let { l.replace(it.value, " ") } ?: singleTimeMatch?.let { l.replace(it.value, " ") } ?: l }
                    .let { l -> DAY_PATTERNS.fold(l) { acc, (_, r) -> acc.replace(r, " ") } }
                    .replace(PARENTHETICAL_NOTE_REGEX, " ")
                val hasLettersOnCurrentLine = lineWithoutTimeAndDay.count { it.isLetter() } >= 2

                if (!hasLettersOnCurrentLine &&
                    i + 1 < lines.size &&
                    TIME_RANGE_REGEX.find(lines[i + 1]) == null &&
                    SINGLE_TIME_REGEX.find(lines[i + 1]) == null &&
                    detectDayOfWeek(lines[i + 1]) == null &&
                    !isTableHeaderRow(lines[i + 1])
                ) {
                    windowLines.add(lines[i + 1])
                    i++
                }
                val combinedBlock = windowLines.joinToString(" | ")

                val blockDay = detectDayOfWeek(combinedBlock) ?: currentDayContext
                currentDayContext = blockDay

                val blockTimeMatch = TIME_RANGE_REGEX.find(combinedBlock)
                val blockSingleTime = if (blockTimeMatch == null) SINGLE_TIME_REGEX.find(combinedBlock) else null

                val startTime: String
                val endTime: String
                if (blockTimeMatch != null) {
                    val rawStart = blockTimeMatch.groupValues[1]
                    val rawEnd = blockTimeMatch.groupValues[2]
                    startTime = normalizeTimeString(rawStart, defaultAmPm = "AM").ifBlank { "09:00 AM" }
                    endTime = normalizeTimeString(rawEnd, defaultAmPm = "AM", afterStart = startTime)
                        .ifBlank { addMinutesToTimeString(startTime, 75) }
                } else if (blockSingleTime != null) {
                    startTime = normalizeTimeString(blockSingleTime.value, defaultAmPm = "AM").ifBlank { "09:00 AM" }
                    endTime = addMinutesToTimeString(startTime, 60)
                } else {
                    startTime = "09:00 AM"
                    endTime = "10:15 AM"
                }

                // Strip parenthetical annotations like "(All A - 406)" before column splitting
                val blockWithoutParens = combinedBlock.replace(PARENTHETICAL_NOTE_REGEX, " ").trim()

                val courseCode = COURSE_CODE_REGEX.find(blockWithoutParens)?.groupValues?.get(1)?.let { cleanCourseCodeField(it) } ?: ""
                val explicitRoomMatch = ROOM_REGEX.find(blockWithoutParens)
                val explicitTeacherMatch = TEACHER_REGEX.find(blockWithoutParens)

                // Remove Day and Time from the row to inspect remaining columns (Subject | Teacher | Room)
                var remainder = blockWithoutParens
                if (blockTimeMatch != null) remainder = remainder.replace(blockTimeMatch.value, " | ")
                if (blockSingleTime != null) remainder = remainder.replace(blockSingleTime.value, " | ")
                DAY_PATTERNS.forEach { (_, regex) ->
                    remainder = remainder.replace(regex, " | ")
                }
                if (explicitRoomMatch != null) remainder = remainder.replace(explicitRoomMatch.value, " | ")
                if (explicitTeacherMatch != null) remainder = remainder.replace(explicitTeacherMatch.value, " | ")
                if (courseCode.isNotBlank()) {
                    COURSE_CODE_REGEX.find(blockWithoutParens)?.let { remainder = remainder.replace(it.value, " | ") }
                }

                // Split remaining cells by pipe "|" first; if only 1 cell with multiple space-separated tokens, inspect tokens
                val pipeCells = remainder.split("|")
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.all { ch -> !ch.isLetterOrDigit() } }

                val cells: List<String> = if (pipeCells.size >= 2) {
                    pipeCells
                } else if (pipeCells.size == 1) {
                    val singleCell = pipeCells.first()
                    val spaceTokens = singleCell.split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (spaceTokens.size >= 2 && BARE_ROOM_TOKEN_REGEX.matches(spaceTokens.last())) {
                        when (spaceTokens.size) {
                            2 -> listOf(spaceTokens[0], spaceTokens[1])
                            3 -> listOf(spaceTokens[0], spaceTokens[1], spaceTokens[2])
                            else -> {
                                val roomTok = spaceTokens.last()
                                val teacherTok = spaceTokens[spaceTokens.size - 2]
                                val subjTok = spaceTokens.dropLast(2).joinToString(" ")
                                listOf(subjTok, teacherTok, roomTok)
                            }
                        }
                    } else {
                        pipeCells
                    }
                } else {
                    emptyList()
                }

                var extractedRoom = cleanRoomField(
                    explicitRoomMatch?.groupValues?.getOrNull(1)?.trim()
                        ?: explicitRoomMatch?.value?.trim()
                        ?: ""
                )
                var extractedTeacher = cleanTeacherField(explicitTeacherMatch?.value?.trim() ?: "")
                val mutableCells = cells.toMutableList()

                // If room not yet found, check if the last cell is a bare room number (e.g., 608, 405, 1001)
                if (extractedRoom.isBlank() && mutableCells.isNotEmpty()) {
                    val lastCell = mutableCells.last()
                    if (BARE_ROOM_TOKEN_REGEX.matches(lastCell)) {
                        extractedRoom = cleanRoomField(lastCell)
                        mutableCells.removeAt(mutableCells.lastIndex)
                    } else {
                        // Check if last cell ends with a 3-4 digit room number (e.g. "THA 608")
                        val trailingRoomMatch = Regex("^(.*?)\\s+(\\d{3,4}[A-Za-z]?)$").find(lastCell)
                        if (trailingRoomMatch != null) {
                            extractedRoom = cleanRoomField(trailingRoomMatch.groupValues[2])
                            mutableCells[mutableCells.lastIndex] = trailingRoomMatch.groupValues[1].trim()
                        }
                    }
                }

                // If teacher not yet found and there are 2+ cells left (Subject | Teacher), take the last cell as Teacher
                if (extractedTeacher.isBlank() && mutableCells.size >= 2) {
                    val candidateTeacher = cleanTeacherField(mutableCells.last())
                    if (candidateTeacher.length in 2..22) {
                        extractedTeacher = candidateTeacher
                        mutableCells.removeAt(mutableCells.lastIndex)
                    }
                }

                val rawSubjectCombined = mutableCells.joinToString(" ")
                val cleanedSubject = cleanCandidateSubjectTitle(rawSubjectCombined)

                val finalSubjectName = when {
                    cleanedSubject.length >= 2 -> cleanedSubject
                    courseCode.isNotBlank() -> courseCode
                    else -> ""
                }

                if (finalSubjectName.isNotBlank() || blockTimeMatch != null) {
                    val displaySubject = finalSubjectName.ifBlank { "Class Session" }
                    val warnings = mutableListOf<String>()
                    var uncertain = false

                    if (finalSubjectName.isBlank()) {
                        uncertain = true
                        warnings.add("Subject name unclear")
                    }
                    if (blockTimeMatch == null && blockSingleTime == null) {
                        uncertain = true
                        warnings.add("Time not detected (defaulted to 09:00 AM)")
                    }

                    val isDup = isDuplicateRoutine(
                        dayOfWeek = blockDay,
                        startTime = startTime,
                        subjectName = displaySubject,
                        existing = existingRoutines,
                        alreadyParsed = results
                    )
                    if (isDup) {
                        warnings.add("Duplicate entry detected for $blockDay at $startTime")
                    }

                    results.add(
                        ScannedRoutineCandidate(
                            subjectName = displaySubject,
                            courseCode = courseCode,
                            teacherName = extractedTeacher,
                            dayOfWeek = blockDay,
                            startTime = startTime,
                            endTime = endTime,
                            roomNumber = extractedRoom,
                            isUncertain = uncertain,
                            isDuplicate = isDup,
                            warningMessage = warnings.joinToString(" - ").ifBlank { null },
                            isSelected = !isDup
                        )
                    )
                }
            }
            i++
        }

        if (results.isEmpty() && lines.isNotEmpty()) {
            val candidateSubject = lines
                .map { cleanCandidateSubjectTitle(it) }
                .firstOrNull { it.length >= 2 && detectDayOfWeek(it) == null && !isBoilerplateLine(it) }
                ?: cleanCandidateSubjectTitle(lines.first())
            results.add(
                ScannedRoutineCandidate(
                    subjectName = candidateSubject.take(45).ifBlank { "Scanned Class" },
                    dayOfWeek = currentDayContext,
                    startTime = "09:00 AM",
                    endTime = "10:00 AM",
                    isUncertain = true,
                    warningMessage = "Partial text recognized - please verify day and time before saving.",
                    isSelected = true
                )
            )
        }

        return results
    }

    fun isDuplicateRoutine(
        dayOfWeek: String,
        startTime: String,
        subjectName: String,
        existing: List<ClassRoutineItem>,
        alreadyParsed: List<ScannedRoutineCandidate> = emptyList()
    ): Boolean {
        val targetStartMins = DateUtils.parseTimeToMinutes(startTime)
        val normSubject = subjectName.trim().lowercase(Locale.US)

        val matchesExisting = existing.any { item ->
            item.dayOfWeek.equals(dayOfWeek, ignoreCase = true) &&
                (DateUtils.parseTimeToMinutes(item.startTime) == targetStartMins ||
                    item.startTime.equals(startTime, ignoreCase = true)) &&
                (item.subjectName.trim().lowercase(Locale.US) == normSubject ||
                    normSubject.contains(item.subjectName.trim().lowercase(Locale.US)) ||
                    item.subjectName.trim().lowercase(Locale.US).contains(normSubject))
        }
        if (matchesExisting) return true

        return alreadyParsed.any { item ->
            item.dayOfWeek.equals(dayOfWeek, ignoreCase = true) &&
                DateUtils.parseTimeToMinutes(item.startTime) == targetStartMins &&
                item.subjectName.trim().lowercase(Locale.US) == normSubject
        }
    }

    /**
     * Parses raw OCR text from an Admit Card or Exam Schedule into structured exam fields.
     */
    fun parseAdmitCardText(rawText: String): ScannedAdmitCardCandidate {
        if (rawText.isBlank()) {
            return ScannedAdmitCardCandidate(
                rawText = "",
                uncertainFields = listOf("No readable text found in image")
            )
        }

        val cleanText = rawText.lines()
            .map { cleanOcrRawLine(it) }
            .filter { it.isNotBlank() }
            .joinToString("\n")
        val lines = cleanText.lines().filter { it.isNotBlank() }

        val rollNo = cleanIdentifierField(ROLL_REGEX.find(cleanText)?.groupValues?.get(1) ?: "")
        val regNo = cleanIdentifierField(REG_REGEX.find(cleanText)?.groupValues?.get(1) ?: "")
        val seatNo = cleanIdentifierField(SEAT_REGEX.find(cleanText)?.groupValues?.get(1) ?: "")
        val building = cleanBuildingField(BUILDING_REGEX.find(cleanText)?.groupValues?.get(1) ?: "")
        val roomMatch = ROOM_REGEX.find(cleanText)
        val roomNo = cleanRoomField(roomMatch?.groupValues?.getOrNull(1) ?: roomMatch?.value ?: "")
        val courseCode = cleanCourseCodeField(COURSE_CODE_REGEX.find(cleanText)?.groupValues?.get(1) ?: "")

        val timeRangeMatch = TIME_RANGE_REGEX.find(cleanText)
        val startTime: String
        val endTime: String
        if (timeRangeMatch != null) {
            startTime = normalizeTimeString(timeRangeMatch.groupValues[1], "AM").ifBlank { "10:00 AM" }
            endTime = normalizeTimeString(timeRangeMatch.groupValues[2], "PM", afterStart = startTime)
                .ifBlank { addMinutesToTimeString(startTime, 180) }
        } else {
            val singleTimes = SINGLE_TIME_REGEX.findAll(cleanText).map { it.value }.toList()
            val firstTime = singleTimes.getOrNull(0)?.let { normalizeTimeString(it, "AM") } ?: ""
            val secondTime = singleTimes.getOrNull(1)?.let { normalizeTimeString(it, "PM", afterStart = firstTime) } ?: ""
            startTime = firstTime
            endTime = if (secondTime.isNotBlank()) {
                secondTime
            } else if (firstTime.isNotBlank()) {
                addMinutesToTimeString(firstTime, 180)
            } else {
                ""
            }
        }

        var dateText = ""
        var parsedDateMillis: Long? = null
        var matchedDateLineIndex = -1
        for ((idx, line) in lines.withIndex()) {
            val found = findDateInText(line)
            if (found != null) {
                dateText = found.first
                parsedDateMillis = found.second
                matchedDateLineIndex = idx
                break
            }
        }
        if (parsedDateMillis == null) {
            findDateInText(cleanText)?.let {
                dateText = it.first
                parsedDateMillis = it.second
            }
        }

        // 1. Check explicit "Subject / Paper / Course: ..." label
        val subjectRegex = Regex(
            "(?:Subject(?:\\s*Name)?|Course(?:\\s*Title|\\s*Name)?|Paper(?:\\s*Name|\\s*Title)?)\\s*[:#=-]\\s*([A-Za-z0-9 &(),/.-]{3,55})",
            RegexOption.IGNORE_CASE
        )
        val explicitSubject = subjectRegex.find(cleanText)?.groupValues?.get(1)?.let {
            cleanCandidateSubjectTitle(it)
        }?.takeIf { it.length >= 3 && !isBoilerplateLine(it) }

        // 2. If the line containing the exam date or course code also contains the subject title (e.g. a table row), extract it!
        val rowBasedSubject = if (matchedDateLineIndex >= 0) {
            val rowText = lines[matchedDateLineIndex]
            extractSubjectFromScheduleRow(rowText, dateText)
        } else {
            null
        }

        // 3. Otherwise pick the best non-boilerplate line
        val heuristicSubject = lines.asSequence()
            .filter { !isBoilerplateLine(it) }
            .map { extractSubjectFromScheduleRow(it, dateText) ?: cleanCandidateSubjectTitle(it) }
            .firstOrNull { candidate ->
                candidate.length in 3..55 &&
                    candidate.any { ch -> ch.isLetter() }
            } ?: ""

        val finalSubject = cleanSubjectField(
            explicitSubject
                ?: rowBasedSubject?.takeIf { it.length >= 3 && !isBoilerplateLine(it) }
                ?: heuristicSubject
        )

        val uncertain = mutableListOf<String>()
        if (finalSubject.isBlank()) uncertain.add("Subject Name")
        if (parsedDateMillis == null) uncertain.add("Exam Date")
        if (startTime.isBlank()) uncertain.add("Exam Time")

        return sanitizeAdmitCardCandidate(
            ScannedAdmitCardCandidate(
                subjectName = finalSubject,
                courseCode = courseCode,
                examDateMillis = parsedDateMillis,
                examDateText = dateText,
                startTime = startTime.ifBlank { "10:00 AM" },
                endTime = endTime.ifBlank { "01:00 PM" },
                examHallBuilding = building,
                roomNumber = roomNo,
                seatNumber = seatNo,
                rollNumber = rollNo,
                registrationNumber = regNo,
                rawText = cleanText,
                uncertainFields = uncertain
            )
        )
    }

    private fun extractSubjectFromScheduleRow(rowText: String, dateText: String): String? {
        var working = rowText
        if (dateText.isNotBlank()) working = working.replace(dateText, " ")
        DATE_REGEX_LIST.forEach { regex -> working = working.replace(regex, " ") }
        TIME_RANGE_REGEX.findAll(working).forEach { m -> working = working.replace(m.value, " ") }
        SINGLE_TIME_REGEX.findAll(working).forEach { m -> working = working.replace(m.value, " ") }
        ROOM_REGEX.findAll(working).forEach { m -> working = working.replace(m.value, " ") }
        COURSE_CODE_REGEX.findAll(working).forEach { m -> working = working.replace(m.value, " ") }
        DAY_PATTERNS.forEach { (_, regex) -> working = working.replace(regex, " ") }
        val cleaned = cleanCandidateSubjectTitle(working)
        return cleaned.takeIf { it.length in 3..55 && !isBoilerplateLine(it) }
    }

    private fun cleanCandidateSubjectTitle(raw: String): String {
        return cleanSubjectField(raw)
    }

    private fun isBoilerplateLine(line: String): Boolean {
        val lower = line.lowercase(Locale.US).trim()
        if (lower.length < 3) return true
        if (BOILERPLATE_KEYWORDS.any { lower.contains(it) }) return true
        if (lower.startsWith("roll") || lower.startsWith("reg") || lower.startsWith("seat") ||
            lower.startsWith("name") || lower.startsWith("student") || lower.startsWith("center") ||
            lower.startsWith("centre") || lower.startsWith("venue")
        ) {
            return true
        }
        // Ignore lines that are mostly digits/punctuation
        val letterCount = lower.count { it.isLetter() }
        return letterCount < 3
    }

    private fun findDateInText(text: String): Pair<String, Long>? {
        for (regex in DATE_REGEX_LIST) {
            val matches = regex.findAll(text)
            for (m in matches) {
                val candidate = m.groupValues[1]
                val millis = tryParseDateMillis(candidate)
                if (millis != null) {
                    return candidate to millis
                }
            }
        }
        return null
    }

    private fun detectDayOfWeek(text: String): String? {
        for ((dayKey, regex) in DAY_PATTERNS) {
            if (regex.containsMatchIn(text)) return dayKey
        }
        return null
    }

    fun normalizeTimeString(raw: String, defaultAmPm: String = "AM", afterStart: String? = null): String {
        val clean = raw.trim().replace(".", ":").uppercase(Locale.US)
        if (clean.isBlank()) return ""
        val hasAm = clean.contains("AM")
        val hasPm = clean.contains("PM")
        val timeDigits = clean.replace("AM", "").replace("PM", "").trim()
        val parts = timeDigits.split(":")
        val hourRaw = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return ""
        val minuteRaw = parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        if (hourRaw > 23 || hourRaw < 0) return ""

        val (hour12, amPm) = when {
            hourRaw == 0 -> 12 to "AM"
            hourRaw in 13..23 -> (hourRaw - 12) to "PM"
            hourRaw == 12 -> 12 to (if (hasAm) "AM" else "PM")
            hasPm -> hourRaw to "PM"
            hasAm -> hourRaw to "AM"
            else -> {
                val startMins = afterStart?.let { DateUtils.parseTimeToMinutes(it) } ?: -1
                val inferred = when {
                    hourRaw in 1..6 -> "PM"
                    hourRaw in 7..11 -> {
                        if (startMins in (7 * 60)..(12 * 60) && hourRaw * 60 < startMins) "PM" else "AM"
                    }
                    else -> defaultAmPm
                }
                hourRaw to inferred
            }
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, minuteRaw, amPm)
    }

    private fun addMinutesToTimeString(startTimeStr: String, durationMinutes: Int): String {
        val startMins = DateUtils.parseTimeToMinutes(startTimeStr)
        if (startMins < 0) return "01:00 PM"
        val total = (startMins + durationMinutes) % (24 * 60)
        return DateUtils.format12HourTime(total / 60, total % 60)
    }

    private fun tryParseDateMillis(dateStr: String): Long? {
        var cleaned = dateStr.trim()
            .replace(Regex("(\\d{1,2})(?:st|nd|rd|th)", RegexOption.IGNORE_CASE), "$1")
            .replace(Regex("\\bof\\b", RegexOption.IGNORE_CASE), " ")
            .replace("Sept", "Sep", ignoreCase = true)
            .replace(".", "/")
            .replace("-", "/")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Expand 2-digit year at the end (e.g. 30/10/26 -> 30/10/2026)
        cleaned = cleaned.replace(Regex("(\\d{1,2}/\\d{1,2}/)(\\d{2})$")) { m ->
            "${m.groupValues[1]}20${m.groupValues[2]}"
        }

        val formats = listOf(
            "dd/MM/yyyy",
            "d/M/yyyy",
            "MM/dd/yyyy",
            "M/d/yyyy",
            "yyyy/MM/dd",
            "yyyy/M/d",
            "MMM d, yyyy",
            "MMM d yyyy",
            "MMMM d, yyyy",
            "MMMM d yyyy",
            "d MMM yyyy",
            "d MMM, yyyy",
            "d MMMM yyyy",
            "d MMMM, yyyy"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = sdf.parse(cleaned)
                if (parsed != null) {
                    return DateUtils.startOfDay(parsed.time)
                }
            } catch (_: Exception) {
            }
        }
        return null
    }
}
