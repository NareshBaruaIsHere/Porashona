package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ClassRoutineItem
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.data.model.Topic

@Database(
    entities = [Subject::class, Topic::class, StudySession::class, ClassRoutineItem::class],
    version = 6,
    exportSchema = false
)
abstract class ExamTrackDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun classRoutineDao(): ClassRoutineDao

    companion object {
        @Volatile
        private var INSTANCE: ExamTrackDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add Exam Hall Vault & readiness target fields to subjects
                db.execSQL("ALTER TABLE subjects ADD COLUMN examHallBuilding TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE subjects ADD COLUMN rollNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE subjects ADD COLUMN referenceLinks TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE subjects ADD COLUMN formulaSheetUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE subjects ADD COLUMN expectedStudyHours REAL NOT NULL DEFAULT 0.0")

                // Add nextReviewAt to topics for Spaced-Repetition Topic Reviser
                db.execSQL("ALTER TABLE topics ADD COLUMN nextReviewAt INTEGER NOT NULL DEFAULT 0")

                // Add courseCode and attendance correction tracking to class_routines
                db.execSQL("ALTER TABLE class_routines ADD COLUMN courseCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE class_routines ADD COLUMN lastAttendanceDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE class_routines ADD COLUMN lastAttendanceStatus INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): ExamTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExamTrackDatabase::class.java,
                    "exam_track_database"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
