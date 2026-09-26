package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.workout.JsonCheckpointFile
import kotlinx.serialization.Serializable
import java.io.File

/**
 * An automatic checkpoint of a running single-exercise workout, so it survives
 * process death. A resume restarts the interrupted set, or returns to the
 * confirmation screen with its unsaved sets.
 */
@Serializable
data class SingleSessionCheckpoint(
    val version: Int = CURRENT_VERSION,
    val exerciseId: Long,
    val totalSets: Int,
    val targetValue: Int,
    val repDuration: Int?,
    val startInterval: Int,
    val intervalDuration: Int,
    val sets: List<WorkoutSet>,
    val comment: String,
    val isAutoMode: Boolean,
    val isDynamicCountSoundEnabled: Boolean,
    val currentSetIndex: Int,
    val atConfirmation: Boolean,
    /** The workout was started from a ToDo task, which is completed when the records are saved. */
    val fromToDo: Boolean,
    val savedAtWallMillis: Long,
) {
    /** Rebuilds the session for [exercise], which must have [exerciseId]. */
    fun toSession(exercise: Exercise): WorkoutSession {
        require(exercise.id == exerciseId) { "Checkpoint is for exercise $exerciseId, not ${exercise.id}" }
        return WorkoutSession(
            exercise = exercise,
            totalSets = totalSets,
            targetValue = targetValue,
            repDuration = repDuration,
            startInterval = startInterval,
            intervalDuration = intervalDuration,
            sets = sets.map { it.copy() }.toMutableList(),
            comment = comment,
            isAutoMode = isAutoMode,
            isDynamicCountSoundEnabled = isDynamicCountSoundEnabled,
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
        const val FILE_NAME = "single-session-checkpoint.json"

        fun of(
            session: WorkoutSession,
            currentSetIndex: Int,
            atConfirmation: Boolean,
            fromToDo: Boolean,
            savedAtWallMillis: Long,
        ) = SingleSessionCheckpoint(
            exerciseId = session.exercise.id,
            totalSets = session.totalSets,
            targetValue = session.targetValue,
            repDuration = session.repDuration,
            startInterval = session.startInterval,
            intervalDuration = session.intervalDuration,
            // copy() snapshots the mutable set fields as they are now
            sets = session.sets.map { it.copy() },
            comment = session.comment,
            isAutoMode = session.isAutoMode,
            isDynamicCountSoundEnabled = session.isDynamicCountSoundEnabled,
            currentSetIndex = currentSetIndex.coerceIn(0, (session.sets.size - 1).coerceAtLeast(0)),
            atConfirmation = atConfirmation,
            fromToDo = fromToDo,
            savedAtWallMillis = savedAtWallMillis,
        )

        fun file(directory: File) =
            JsonCheckpointFile(File(directory, FILE_NAME), serializer(), CURRENT_VERSION)
    }
}
