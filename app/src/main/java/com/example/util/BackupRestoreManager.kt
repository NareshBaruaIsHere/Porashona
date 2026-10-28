package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.model.ClassRoutineItem
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.Topic
import com.example.data.repository.ExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class BackupSubjectDto(
    val id: String,
    val name: String,
    val examDate: Long,
    val colorTag: String = "#0D9488",
    val startTime: String = "",
    val endTime: String = "",
    val roomNumber: String = "",
    val examHallBuilding: String = "",
    val seatNumber: String = "",
    val rollNumber: String = "",
    val admitCardNumber: String = "",
    val formulaNotes: String = "",
    val driveLink: String = "",
    val referenceLinks: String = "",
    val admitCardPhotoUri: String = "",
    val formulaSheetUri: String = "",
    val expectedStudyHours: Float = 6f,
    val createdAt: Long = 0L
) {
    fun toEntity(): Subject = Subject(
        id = id,
        name = name,
        examDate = examDate,
        colorTag = colorTag,
        startTime = startTime,
        endTime = endTime,
        roomNumber = roomNumber,
        examHallBuilding = examHallBuilding,
        seatNumber = seatNumber,
        rollNumber = rollNumber,
        admitCardNumber = admitCardNumber,
        formulaNotes = formulaNotes,
        driveLink = driveLink,
        referenceLinks = referenceLinks,
        admitCardPhotoUri = admitCardPhotoUri,
        formulaSheetUri = formulaSheetUri,
        expectedStudyHours = expectedStudyHours,
        createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis()
    )

    companion object {
        fun fromEntity(subject: Subject): BackupSubjectDto = BackupSubjectDto(
            id = subject.id,
            name = subject.name,
            examDate = subject.examDate,
            colorTag = subject.colorTag,
            startTime = subject.startTime,
            endTime = subject.endTime,
            roomNumber = subject.roomNumber,
            examHallBuilding = subject.examHallBuilding,
            seatNumber = subject.seatNumber,
            rollNumber = subject.rollNumber,
            admitCardNumber = subject.admitCardNumber,
            formulaNotes = subject.formulaNotes,
            driveLink = subject.driveLink,
            referenceLinks = subject.referenceLinks,
            admitCardPhotoUri = subject.admitCardPhotoUri,
            formulaSheetUri = subject.formulaSheetUri,
            expectedStudyHours = subject.expectedStudyHours,
            createdAt = subject.createdAt
        )
    }
}

@Serializable
data class BackupTopicDto(
    val id: String,
    val subjectId: String,
    val title: String,
    val isDone: Boolean = false,
    val order: Int = 0,
    val completedAt: Long = 0L,
    val lastReviewedAt: Long = 0L,
    val reviewCount: Int = 0,
    val nextReviewAt: Long = 0L
) {
    fun toEntity(): Topic = Topic(
        id = id,
        subjectId = subjectId,
        title = title,
        isDone = isDone,
        order = order,
        completedAt = completedAt,
        lastReviewedAt = lastReviewedAt,
        reviewCount = reviewCount,
        nextReviewAt = nextReviewAt
    )

    companion object {
        fun fromEntity(topic: Topic): BackupTopicDto = BackupTopicDto(
            id = topic.id,
            subjectId = topic.subjectId,
            title = topic.title,
            isDone = topic.isDone,
            order = topic.order,
            completedAt = topic.completedAt,
            lastReviewedAt = topic.lastReviewedAt,
            reviewCount = topic.reviewCount,
            nextReviewAt = topic.nextReviewAt
        )
    }
}

@Serializable
data class BackupStudySessionDto(
    val id: String,
    val subjectId: String? = null,
    val subjectName: String = "General Study",
    val subjectColor: String = "#0D9488",
    val durationMinutes: Int,
    val dateMillis: Long,
    val notes: String = "",
    val sessionType: String = "MANUAL",
    val createdAt: Long = 0L
) {
    fun toEntity(): StudySession = StudySession(
        id = id,
        subjectId = subjectId,
        subjectName = subjectName,
        subjectColor = subjectColor,
        durationMinutes = durationMinutes,
        dateMillis = dateMillis,
        notes = notes,
        sessionType = sessionType,
        createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis()
    )

    companion object {
        fun fromEntity(session: StudySession): BackupStudySessionDto = BackupStudySessionDto(
            id = session.id,
            subjectId = session.subjectId,
            subjectName = session.subjectName,
            subjectColor = session.subjectColor,
            durationMinutes = session.durationMinutes,
            dateMillis = session.dateMillis,
            notes = session.notes,
            sessionType = session.sessionType,
            createdAt = session.createdAt
        )
    }
}

@Serializable
data class BackupClassRoutineDto(
    val id: String,
    val subjectName: String,
    val courseCode: String = "",
    val teacherName: String = "",
    val roomNumber: String = "",
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val colorTag: String = "#0284C7",
    val notes: String = "",
    val attendedCount: Int = 0,
    val missedCount: Int = 0,
    val lastAttendanceDate: Long = 0L,
    val lastAttendanceStatus: Int = 0,
    val createdAt: Long = 0L
) {
    fun toEntity(): ClassRoutineItem = ClassRoutineItem(
        id = id,
        subjectName = subjectName,
        courseCode = courseCode,
        teacherName = teacherName,
        roomNumber = roomNumber,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        colorTag = colorTag,
        notes = notes,
        attendedCount = attendedCount,
        missedCount = missedCount,
        lastAttendanceDate = lastAttendanceDate,
        lastAttendanceStatus = lastAttendanceStatus,
        createdAt = if (createdAt > 0L) createdAt else System.currentTimeMillis()
    )

    companion object {
        fun fromEntity(item: ClassRoutineItem): BackupClassRoutineDto = BackupClassRoutineDto(
            id = item.id,
            subjectName = item.subjectName,
            courseCode = item.courseCode,
            teacherName = item.teacherName,
            roomNumber = item.roomNumber,
            dayOfWeek = item.dayOfWeek,
            startTime = item.startTime,
            endTime = item.endTime,
            colorTag = item.colorTag,
            notes = item.notes,
            attendedCount = item.attendedCount,
            missedCount = item.missedCount,
            lastAttendanceDate = item.lastAttendanceDate,
            lastAttendanceStatus = item.lastAttendanceStatus,
            createdAt = item.createdAt
        )
    }
}

@Serializable
data class ExamTrackBackupPayload(
    val schemaVersion: Int = 3,
    val appName: String = "Porashona",
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val subjects: List<BackupSubjectDto> = emptyList(),
    val topics: List<BackupTopicDto> = emptyList(),
    val studySessions: List<BackupStudySessionDto> = emptyList(),
    val classRoutines: List<BackupClassRoutineDto> = emptyList()
)

data class BackupRestoreSummary(
    val subjectsCount: Int,
    val topicsCount: Int,
    val sessionsCount: Int,
    val routinesCount: Int
)

object BackupRestoreManager {

    private val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun defaultBackupFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val datePart = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(nowMillis))
        return "porashona_backup_$datePart.json"
    }

    suspend fun createBackupJsonString(repository: ExamRepository): String = withContext(Dispatchers.IO) {
        val subjects = repository.getAllSubjects().map { BackupSubjectDto.fromEntity(it) }
        val topics = repository.getAllTopicsSync().map { BackupTopicDto.fromEntity(it) }
        val sessions = repository.getAllStudySessionsSync().map { BackupStudySessionDto.fromEntity(it) }
        val routines = repository.getAllClassRoutinesSync().map { BackupClassRoutineDto.fromEntity(it) }

        val payload = ExamTrackBackupPayload(
            schemaVersion = 3,
            appName = "Porashona",
            exportedAtMillis = System.currentTimeMillis(),
            subjects = subjects,
            topics = topics,
            studySessions = sessions,
            classRoutines = routines
        )
        jsonFormat.encodeToString(ExamTrackBackupPayload.serializer(), payload)
    }

    suspend fun exportBackupToUri(
        context: Context,
        uri: Uri,
        repository: ExamRepository
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val subjects = repository.getAllSubjects().map { BackupSubjectDto.fromEntity(it) }
            val topics = repository.getAllTopicsSync().map { BackupTopicDto.fromEntity(it) }
            val sessions = repository.getAllStudySessionsSync().map { BackupStudySessionDto.fromEntity(it) }
            val routines = repository.getAllClassRoutinesSync().map { BackupClassRoutineDto.fromEntity(it) }

            val payload = ExamTrackBackupPayload(
                schemaVersion = 3,
                appName = "Porashona",
                exportedAtMillis = System.currentTimeMillis(),
                subjects = subjects,
                topics = topics,
                studySessions = sessions,
                classRoutines = routines
            )
            val jsonContent = jsonFormat.encodeToString(ExamTrackBackupPayload.serializer(), payload)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Unable to open file output stream")

            BackupRestoreSummary(
                subjectsCount = subjects.size,
                topicsCount = topics.size,
                sessionsCount = sessions.size,
                routinesCount = routines.size
            )
        }
    }

    suspend fun restoreBackupFromJsonString(
        jsonString: String,
        repository: ExamRepository,
        clearBeforeRestore: Boolean = false
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = jsonFormat.decodeFromString(ExamTrackBackupPayload.serializer(), jsonString)
            val subjects = payload.subjects.map { it.toEntity() }
            val subjectIds = subjects.map { it.id }.toSet()
            val existingSubjectIds = repository.getAllSubjects().map { it.id }.toSet()
            val validParentIds = if (clearBeforeRestore) subjectIds else (subjectIds + existingSubjectIds)

            val topics = payload.topics
                .filter { it.subjectId in validParentIds }
                .map { it.toEntity() }
            val sessions = payload.studySessions.map { it.toEntity() }
            val routines = payload.classRoutines.map { it.toEntity() }

            repository.restoreBackupData(
                subjects = subjects,
                topics = topics,
                sessions = sessions,
                routines = routines,
                clearBeforeRestore = clearBeforeRestore
            )

            BackupRestoreSummary(
                subjectsCount = subjects.size,
                topicsCount = topics.size,
                sessionsCount = sessions.size,
                routinesCount = routines.size
            )
        }
    }

    suspend fun importBackupFromUri(
        context: Context,
        uri: Uri,
        repository: ExamRepository,
        clearBeforeRestore: Boolean = false
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val rawJson = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Unable to read backup file")

            restoreBackupFromJsonString(
                jsonString = rawJson,
                repository = repository,
                clearBeforeRestore = clearBeforeRestore
            ).getOrThrow()
        }
    }
}
