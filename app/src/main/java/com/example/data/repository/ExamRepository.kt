package com.example.data.repository

import com.example.data.local.ClassRoutineDao
import com.example.data.local.StudySessionDao
import com.example.data.local.SubjectDao
import com.example.data.local.TopicDao
import com.example.data.model.ClassRoutineItem
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.SubjectWithTopics
import com.example.data.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow

class ExamRepository(
    private val subjectDao: SubjectDao,
    private val topicDao: TopicDao,
    private val studySessionDao: StudySessionDao,
    private val classRoutineDao: ClassRoutineDao? = null
) {
    val allSubjectsWithTopics: Flow<List<SubjectWithTopics>> = combine(
        subjectDao.getAllSubjects(),
        topicDao.getAllTopics()
    ) { subjects, topics ->
        val topicsBySubject = topics.groupBy { it.subjectId }
        subjects.map { subject ->
            SubjectWithTopics(
                subject = subject,
                topics = topicsBySubject[subject.id]?.sortedBy { it.order } ?: emptyList()
            )
        }
    }

    val allStudySessions: Flow<List<StudySession>> = studySessionDao.getAllStudySessions()

    fun getStudySessionsForSubject(subjectId: String): Flow<List<StudySession>> {
        return studySessionDao.getStudySessionsForSubject(subjectId)
    }

    fun getStudySessionsSince(sinceMillis: Long): Flow<List<StudySession>> {
        return studySessionDao.getStudySessionsSince(sinceMillis)
    }

    fun getStudySessionsBetween(startMillis: Long, endMillis: Long): Flow<List<StudySession>> {
        return studySessionDao.getStudySessionsBetween(startMillis, endMillis)
    }

    suspend fun insertStudySession(session: StudySession) {
        studySessionDao.insertStudySession(session)
    }

    suspend fun deleteStudySessionById(id: String) {
        studySessionDao.deleteStudySessionById(id)
    }

    suspend fun getAllStudySessionsSync(): List<StudySession> {
        return studySessionDao.getAllStudySessionsSync()
    }

    fun getSubjectWithTopics(subjectId: String): Flow<SubjectWithTopics?> = combine(
        subjectDao.getSubjectById(subjectId),
        topicDao.getTopicsForSubject(subjectId)
    ) { subject, topics ->
        subject?.let {
            SubjectWithTopics(
                subject = it,
                topics = topics.sortedBy { t -> t.order }
            )
        }
    }

    suspend fun getSubjectById(subjectId: String): Subject? {
        return subjectDao.getSubjectByIdSync(subjectId)
    }

    suspend fun getAllSubjects(): List<Subject> {
        return subjectDao.getAllSubjectsSync()
    }

    suspend fun getAllTopicsSync(): List<Topic> {
        return topicDao.getAllTopicsSync()
    }

    suspend fun insertSubject(subject: Subject) {
        subjectDao.insertSubject(subject)
    }

    suspend fun updateSubject(subject: Subject) {
        subjectDao.updateSubject(subject)
    }

    suspend fun deleteSubject(subject: Subject) {
        topicDao.deleteTopicsForSubject(subject.id)
        subjectDao.deleteSubject(subject)
    }

    suspend fun deleteSubjectById(subjectId: String) {
        topicDao.deleteTopicsForSubject(subjectId)
        subjectDao.deleteSubjectById(subjectId)
    }

    suspend fun insertTopic(topic: Topic) {
        topicDao.insertTopic(topic)
    }

    suspend fun updateTopic(topic: Topic) {
        topicDao.updateTopic(topic)
    }

    suspend fun deleteTopicById(topicId: String) {
        topicDao.deleteTopicById(topicId)
    }

    suspend fun updateTopicsOrder(topics: List<Topic>) {
        topics.forEachIndexed { index, topic ->
            if (topic.order != index) {
                topicDao.updateTopic(topic.copy(order = index))
            }
        }
    }

    // Class Routine
    val allClassRoutines: Flow<List<ClassRoutineItem>> =
        classRoutineDao?.getAllRoutines() ?: emptyFlow()

    fun getClassRoutinesForDay(day: String): Flow<List<ClassRoutineItem>> =
        classRoutineDao?.getRoutinesForDay(day) ?: emptyFlow()

    suspend fun getAllClassRoutinesSync(): List<ClassRoutineItem> =
        classRoutineDao?.getAllRoutinesSync() ?: emptyList()

    suspend fun insertClassRoutine(item: ClassRoutineItem) {
        classRoutineDao?.insertRoutine(item)
    }

    suspend fun updateClassRoutine(item: ClassRoutineItem) {
        classRoutineDao?.updateRoutine(item)
    }

    suspend fun updateClassRoutines(items: List<ClassRoutineItem>) {
        classRoutineDao?.updateRoutines(items)
    }

    suspend fun deleteClassRoutineById(id: String) {
        classRoutineDao?.deleteRoutineById(id)
    }

    suspend fun restoreBackupData(
        subjects: List<Subject>,
        topics: List<Topic>,
        sessions: List<StudySession>,
        routines: List<ClassRoutineItem>,
        clearBeforeRestore: Boolean = false
    ) {
        if (clearBeforeRestore) {
            clearAllData()
        }
        if (subjects.isNotEmpty()) {
            subjectDao.insertSubjects(subjects)
        }
        if (topics.isNotEmpty()) {
            topicDao.insertTopics(topics)
        }
        if (sessions.isNotEmpty()) {
            studySessionDao.insertStudySessions(sessions)
        }
        if (routines.isNotEmpty()) {
            classRoutineDao?.insertRoutines(routines)
        }
    }

    suspend fun clearAllData() {
        topicDao.clearAllTopics()
        subjectDao.clearAllSubjects()
        studySessionDao.clearAllSessions()
        classRoutineDao?.clearAllRoutines()
    }
}
