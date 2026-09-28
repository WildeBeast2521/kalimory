package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet

/**
 * Format training records for clipboard copy
 *
 * Example: "Push-up: 12/11/10, Bridge: 20/20/20/30, Plank: 35s/32s/30s"
 *
 * Rules:
 * - Group by exercise
 * - Within each exercise, sort by time + set number
 * - Merge multiple sessions into one
 * - Unilateral: R6 L5/R5 L4 (セット間をスラッシュ)
 * - Isometric: 35s/32s/30s
 */
fun formatRecordsForClipboard(
    records: List<HistorySet>,
    exercises: List<Exercise>
): String {
    if (records.isEmpty()) return ""

    val exerciseMap = exercises.associateBy { it.id }

    // Group by exercise and sort by time + set number
    val recordsByExercise = records
        .groupBy { it.exerciseId }
        .mapValues { (_, recs) ->
            recs.sortedWith(compareBy({ it.time }, { it.setNumber }))
        }

    return recordsByExercise.entries.joinToString(", ") { (exerciseId, sortedRecords) ->
        val exercise = exerciseMap[exerciseId] ?: return@joinToString ""
        val exerciseName = exercise.name

        val valuesText = when {
            // Unilateral exercise - format: R6 L5/R5 L4
            exercise.laterality == "Unilateral" -> {
                val pairs = sortedRecords.map { record ->
                    "R${record.valueRight}" + (record.valueLeft?.let { " L$it" } ?: "")
                }
                pairs.joinToString("/")
            }

            // Isometric exercise - format: 35s/32s/30s
            exercise.type == "Isometric" -> {
                val values = sortedRecords.map { "${it.valueRight}s" }
                values.joinToString("/")
            }

            // Dynamic Bilateral exercise - format: 12/11/10
            else -> {
                val values = sortedRecords.map { it.valueRight.toString() }
                values.joinToString("/")
            }
        }

        "$exerciseName: $valuesText"
    }
}
