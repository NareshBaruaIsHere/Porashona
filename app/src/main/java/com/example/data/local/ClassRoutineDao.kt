package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ClassRoutineItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassRoutineDao {
    @Query("SELECT * FROM class_routines ORDER BY startTime ASC")
    fun getAllRoutines(): Flow<List<ClassRoutineItem>>

    @Query("SELECT * FROM class_routines ORDER BY startTime ASC")
    suspend fun getAllRoutinesSync(): List<ClassRoutineItem>

    @Query("SELECT * FROM class_routines WHERE dayOfWeek = :day ORDER BY startTime ASC")
    fun getRoutinesForDay(day: String): Flow<List<ClassRoutineItem>>

    @Query("SELECT * FROM class_routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: String): ClassRoutineItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(item: ClassRoutineItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(items: List<ClassRoutineItem>)

    @Update
    suspend fun updateRoutine(item: ClassRoutineItem)

    @Update
    suspend fun updateRoutines(items: List<ClassRoutineItem>)

    @Delete
    suspend fun deleteRoutine(item: ClassRoutineItem)

    @Query("DELETE FROM class_routines WHERE id = :id")
    suspend fun deleteRoutineById(id: String)

    @Query("DELETE FROM class_routines")
    suspend fun clearAllRoutines()
}
