package com.example.gymtracking.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "exercise_definitions")
data class ExerciseDefinitionEntity(
    @PrimaryKey val name: String,
    val category: String,
    val localImagePath: String? = null
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val date: String // ISO-8601 format
)

@Entity(tableName = "exercise_logs")
data class ExerciseLogEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val name: String,
    val category: String,
    val localImagePath: String? = null
)

@Entity(tableName = "workout_sets")
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val logId: String,
    val reps: Int = 0,
    val weight: Float = 0f,
    val timeMinutes: Int = 0,
    val distanceKm: Float = 0f
)

@Entity(tableName = "body_weight")
data class BodyWeightEntity(
    @PrimaryKey val date: String, // ISO-8601 date
    val weight: Float
)

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefinition(definition: ExerciseDefinitionEntity)

    @Update
    suspend fun updateDefinition(definition: ExerciseDefinitionEntity)

    @Query("SELECT * FROM exercise_definitions")
    suspend fun getAllDefinitions(): List<ExerciseDefinitionEntity>

    @Query("DELETE FROM exercise_definitions WHERE name = :name")
    suspend fun deleteDefinition(name: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions")
    suspend fun getAllSessions(): List<WorkoutSessionEntity>

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExerciseLogEntity)

    @Update
    suspend fun updateLog(log: ExerciseLogEntity)

    @Query("SELECT * FROM exercise_logs WHERE sessionId = :sessionId")
    suspend fun getLogsForSession(sessionId: String): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_logs")
    suspend fun getAllLogs(): List<ExerciseLogEntity>

    @Query("DELETE FROM exercise_logs WHERE id = :logId")
    suspend fun deleteLog(logId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(workoutSet: WorkoutSetEntity)

    @Update
    suspend fun updateSet(workoutSet: WorkoutSetEntity)

    @Query("SELECT * FROM workout_sets WHERE logId = :logId")
    suspend fun getSetsForLog(logId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE id = :setId")
    suspend fun getSetById(setId: String): WorkoutSetEntity?

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteSet(setId: String)
    
    @Query("SELECT * FROM workout_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionByDate(date: String): WorkoutSessionEntity?

    @Query("SELECT COUNT(*) FROM exercise_definitions")
    suspend fun getDefinitionsCount(): Int

    // Body Weight
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeight(entry: BodyWeightEntity)

    @Query("SELECT * FROM body_weight ORDER BY date DESC")
    suspend fun getAllBodyWeights(): List<BodyWeightEntity>
}

@Database(entities = [ExerciseDefinitionEntity::class, WorkoutSessionEntity::class, ExerciseLogEntity::class, WorkoutSetEntity::class, BodyWeightEntity::class], version = 3)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN timeMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sets ADD COLUMN distanceKm REAL NOT NULL DEFAULT 0.0")
            }
        }

        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
