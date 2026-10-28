package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Query("SELECT * FROM study_sessions ORDER BY dateMillis DESC")
    fun getAllStudySessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions ORDER BY dateMillis DESC")
    suspend fun getAllStudySessionsSync(): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE dateMillis >= :sinceMillis ORDER BY dateMillis DESC")
    fun getStudySessionsSince(sinceMillis: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE dateMillis BETWEEN :startMillis AND :endMillis ORDER BY dateMillis DESC")
    fun getStudySessionsBetween(startMillis: Long, endMillis: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId ORDER BY dateMillis DESC")
    fun getStudySessionsForSubject(subjectId: String): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySessions(sessions: List<StudySession>)

    @Delete
    suspend fun deleteStudySession(session: StudySession)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteStudySessionById(id: String)

    @Query("DELETE FROM study_sessions WHERE subjectId = :subjectId")
    suspend fun deleteStudySessionsForSubject(subjectId: String)

    @Query("DELETE FROM study_sessions")
    suspend fun clearAllSessions()
}
