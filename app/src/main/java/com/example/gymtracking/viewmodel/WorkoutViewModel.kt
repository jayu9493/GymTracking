package com.example.gymtracking.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracking.data.*
import com.example.gymtracking.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.*

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WorkoutDatabase.getDatabase(application)
    private val dao = db.workoutDao()
    private val prefs = application.getSharedPreferences("gym_tracking_prefs", Context.MODE_PRIVATE)

    private val _sessions = mutableStateListOf<WorkoutSession>()
    val sessions: List<WorkoutSession> get() = _sessions

    private val _exerciseDefinitions = mutableStateListOf<ExerciseDefinition>()
    val exerciseDefinitions: List<ExerciseDefinition> get() = _exerciseDefinitions
    
    private val _bodyWeights = mutableStateListOf<BodyWeightEntity>()
    val bodyWeights: List<BodyWeightEntity> get() = _bodyWeights

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            // 1. Load Exercise Definitions
            val defsFromDb = dao.getAllDefinitions()
            val isFirstRun = prefs.getBoolean("first_run", true)

            if (defsFromDb.isEmpty() && isFirstRun) {
                val initial = listOf(
                    ExerciseDefinition("Bench Press", "Chest"),
                    ExerciseDefinition("Squats", "Legs"),
                    ExerciseDefinition("Bicep Curls", "Arms"),
                    ExerciseDefinition("Treadmill", "Cardio"),
                    ExerciseDefinition("Cycling", "Cardio")
                )
                initial.forEach { 
                    dao.insertDefinition(ExerciseDefinitionEntity(it.name, it.category, it.gifUrl))
                }
                prefs.edit().putBoolean("first_run", false).apply()
                _exerciseDefinitions.clear()
                _exerciseDefinitions.addAll(initial)
            } else {
                _exerciseDefinitions.clear()
                _exerciseDefinitions.addAll(defsFromDb.map { 
                    ExerciseDefinition(it.name, it.category, it.localImagePath) 
                })
            }

            // 2. Load Sessions with CLEANUP
            val sessionEntities = dao.getAllSessions()
            val sessionsList = mutableListOf<WorkoutSession>()
            for (sEntity in sessionEntities) {
                val logs = dao.getLogsForSession(sEntity.id)
                val exerciseLogs = mutableListOf<ExerciseLog>()
                for (lEntity in logs) {
                    val sets = dao.getSetsForLog(lEntity.id).map { 
                        WorkoutSet(it.id, it.reps, it.weight, it.timeMinutes, it.distanceKm) 
                    }
                    if (sets.isNotEmpty()) {
                        exerciseLogs.add(ExerciseLog(lEntity.id, lEntity.name, lEntity.category, lEntity.localImagePath, sets))
                    } else {
                        dao.deleteLog(lEntity.id)
                    }
                }
                if (exerciseLogs.isNotEmpty()) {
                    sessionsList.add(WorkoutSession(sEntity.id, LocalDate.parse(sEntity.date), exerciseLogs))
                } else {
                    dao.deleteSession(sEntity.id)
                }
            }
            _sessions.clear()
            _sessions.addAll(sessionsList.sortedByDescending { it.date })
            
            // 3. Load Body Weights
            _bodyWeights.clear()
            _bodyWeights.addAll(dao.getAllBodyWeights().sortedByDescending { it.date })
            
            exportToCSV()
        }
    }

    fun bulkImportExercises(rootUri: Uri, onProgress: (String) -> Unit, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@launch
            
            prefs.edit().putBoolean("first_run", false).apply()

            rootDoc.listFiles().forEach { categoryDoc ->
                if (categoryDoc.isDirectory) {
                    val categoryName = categoryDoc.name ?: "Uncategorized"
                    categoryDoc.listFiles().forEach { exerciseDoc ->
                        if (!exerciseDoc.isDirectory) {
                            val exerciseName = exerciseDoc.name?.substringBeforeLast(".") ?: "Unknown"
                            val localPath = saveImageToInternalStorage(exerciseDoc.uri, exerciseName)
                            dao.insertDefinition(ExerciseDefinitionEntity(exerciseName, categoryName, localPath))
                            
                            withContext(Dispatchers.Main) {
                                onProgress("Imported $exerciseName into $categoryName")
                            }
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                loadData()
                onComplete()
            }
        }
    }

    fun addExerciseDefinition(name: String, category: String, uri: Uri?) {
        viewModelScope.launch {
            prefs.edit().putBoolean("first_run", false).apply()
            var localPath: String? = null
            uri?.let {
                localPath = saveImageToInternalStorage(it, name)
            }
            val entity = ExerciseDefinitionEntity(name, category, localPath)
            dao.insertDefinition(entity)
            loadData()
        }
    }

    fun updateExerciseDefinition(oldName: String, newName: String, newCategory: String, newUri: Uri?) {
        viewModelScope.launch {
            val oldEntity = dao.getAllDefinitions().find { it.name == oldName } ?: return@launch
            var localPath = oldEntity.localImagePath
            
            if (newUri != null) {
                localPath = saveImageToInternalStorage(newUri, newName)
                oldEntity.localImagePath?.let { File(it).delete() }
            }
            
            if (oldName != newName) {
                dao.deleteDefinition(oldName)
            }
            
            dao.insertDefinition(ExerciseDefinitionEntity(newName, newCategory, localPath))

            // Cascade rename to historical logs
            if (oldName != newName) {
                val allLogs = dao.getAllLogs()
                allLogs.filter { it.name == oldName }.forEach { log ->
                    dao.updateLog(log.copy(name = newName))
                }
            }

            loadData()
        }
    }

    fun deleteExerciseDefinition(definition: ExerciseDefinition) {
        viewModelScope.launch {
            dao.deleteDefinition(definition.name)
            definition.gifUrl?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
            loadData()
        }
    }

    private fun saveImageToInternalStorage(uri: Uri, fileName: String): String? {
        val context = getApplication<Application>().applicationContext
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.filesDir, "exercises")
            if (!file.exists()) file.mkdirs()
            
            val mimeType = context.contentResolver.getType(uri)
            val extension = when {
                mimeType?.contains("png", ignoreCase = true) == true -> ".png"
                mimeType?.contains("jpeg", ignoreCase = true) == true || mimeType?.contains("jpg", ignoreCase = true) == true -> ".jpg"
                mimeType?.contains("webp", ignoreCase = true) == true -> ".webp"
                else -> ".gif"
            }
            val outFile = File(file, "${fileName.replace(" ", "_")}_${System.currentTimeMillis()}$extension")
            val outputStream = FileOutputStream(outFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            outFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun addExercise(
        date: LocalDate,
        definition: ExerciseDefinition,
        reps: Int = 0,
        weight: Float = 0f,
        timeMinutes: Int = 0,
        distanceKm: Float = 0f
    ) {
        viewModelScope.launch {
            val dateStr = date.toString()
            var sessionEntity = dao.getSessionByDate(dateStr)
            
            if (sessionEntity == null) {
                sessionEntity = WorkoutSessionEntity(UUID.randomUUID().toString(), dateStr)
                dao.insertSession(sessionEntity)
            }

            val logs = dao.getLogsForSession(sessionEntity.id)
            var logEntity = logs.find { it.name == definition.name }
            
            if (logEntity == null) {
                logEntity = ExerciseLogEntity(
                    UUID.randomUUID().toString(), 
                    sessionEntity.id, 
                    definition.name, 
                    definition.category, 
                    definition.gifUrl
                )
                dao.insertLog(logEntity)
            }

            val setEntity = WorkoutSetEntity(
                id = UUID.randomUUID().toString(),
                logId = logEntity.id,
                reps = reps,
                weight = weight,
                timeMinutes = timeMinutes,
                distanceKm = distanceKm
            )
            dao.insertSet(setEntity)
            
            loadData()
        }
    }

    fun updateSet(
        setId: String,
        reps: Int = 0,
        weight: Float = 0f,
        timeMinutes: Int = 0,
        distanceKm: Float = 0f
    ) {
        viewModelScope.launch {
            val setEntity = dao.getSetById(setId)
            if (setEntity != null) {
                dao.updateSet(setEntity.copy(
                    reps = reps,
                    weight = weight,
                    timeMinutes = timeMinutes,
                    distanceKm = distanceKm
                ))
                loadData()
            }
        }
    }

    fun deleteSet(setId: String) {
        viewModelScope.launch {
            dao.deleteSet(setId)
            loadData() 
        }
    }
    
    fun addBodyWeight(date: LocalDate, weight: Float) {
        viewModelScope.launch {
            dao.insertBodyWeight(BodyWeightEntity(date.toString(), weight))
            loadData()
        }
    }

    private suspend fun exportToCSV() {
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val csvFile = File(context.filesDir, "workout_history.csv")
            val writer = FileOutputStream(csvFile).bufferedWriter()
            
            writer.write("Date,Exercise,Category,SetNumber,Reps,WeightKg,TimeMinutes,DistanceKm\n")
            
            sessions.forEach { session ->
                session.exercises.forEach { exercise ->
                    exercise.sets.forEachIndexed { index, set ->
                        val exerciseName = exercise.name.replace("\"", "\"\"")
                        writer.write("${session.date},\"$exerciseName\",${exercise.category},${index + 1},${set.reps},${set.weight},${set.timeMinutes},${set.distanceKm}\n")
                    }
                }
            }
            writer.close()
        }
    }
    
    fun getPersonalBest(exerciseName: String): Float {
        return sessions.flatMap { it.exercises }
            .filter { it.name == exerciseName }
            .flatMap { it.sets }
            .maxOfOrNull { it.weight } ?: 0f
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete all sets for each exercise log
            session.exercises.forEach { exercise ->
                exercise.sets.forEach { set ->
                    dao.deleteSet(set.id)
                }
                dao.deleteLog(exercise.id)
            }
            dao.deleteSession(session.id)
            withContext(Dispatchers.Main) {
                _sessions.removeAll { it.id == session.id }
            }
        }
    }

    fun getLastUsedWeightAndReps(exerciseName: String): Pair<Float, Int> {
        val lastSession = sessions.sortedByDescending { it.date }
            .firstOrNull { s -> s.exercises.any { it.name == exerciseName } }
        val lastExercise = lastSession?.exercises?.firstOrNull { it.name == exerciseName }
        val lastSet = lastExercise?.sets?.lastOrNull()
        return Pair(lastSet?.weight ?: 40f, lastSet?.reps ?: 10)
    }
}
