package com.example.gymtracking.model

import java.time.LocalDate

data class ExerciseDefinition(
    val name: String,
    val category: String,
    val gifUrl: String? = null
)

data class WorkoutSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val date: LocalDate = LocalDate.now(),
    val exercises: List<ExerciseLog> = emptyList()
)

data class ExerciseLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val gifUrl: String? = null,
    val sets: List<WorkoutSet> = emptyList()
)

data class WorkoutSet(
    val id: String = java.util.UUID.randomUUID().toString(),
    val reps: Int = 0,
    val weight: Float = 0f,
    val timeMinutes: Int = 0,
    val distanceKm: Float = 0f
)
