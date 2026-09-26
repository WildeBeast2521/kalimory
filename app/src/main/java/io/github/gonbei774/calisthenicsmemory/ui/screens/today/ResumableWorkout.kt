package io.github.gonbei774.calisthenicsmemory.ui.screens.today

import android.content.Context
import io.github.gonbei774.calisthenicsmemory.data.ProgramSessionCheckpoint
import io.github.gonbei774.calisthenicsmemory.data.ProgramSessionCheckpointStore
import io.github.gonbei774.calisthenicsmemory.data.SavedWorkoutState
import io.github.gonbei774.calisthenicsmemory.ui.screens.INTERVAL_CHECKPOINT_FILE
import io.github.gonbei774.calisthenicsmemory.ui.screens.SingleSessionCheckpoint
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutCheckpoint
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutCheckpointStore
import java.io.File

/**
 * A workout that can be continued. Opening the owning screen offers the resume,
 * through the same dialogs and checks as before, so this only points the way.
 */
sealed interface ResumableWorkout {
    val savedAtWallMillis: Long

    data class Single(val exerciseId: Long, override val savedAtWallMillis: Long) : ResumableWorkout

    /** [savedByUser] is the program "Save & Exit" slot; otherwise the automatic checkpoint. */
    data class Program(
        val programId: Long,
        val savedByUser: Boolean,
        override val savedAtWallMillis: Long,
    ) : ResumableWorkout

    data class Interval(val programId: Long, override val savedAtWallMillis: Long) : ResumableWorkout
}

/** Everything the resume slots hold, read once. */
data class ResumeSources(
    val single: SingleSessionCheckpoint?,
    val program: ProgramSessionCheckpoint?,
    val savedProgramId: Long?,
    val savedProgramAtWallMillis: Long,
    val interval: WorkoutCheckpoint?,
) {
    companion object {
        /** Reads the files and preferences; call off the main thread. Unreadable slots count as empty. */
        fun load(context: Context): ResumeSources {
            val directory = context.filesDir
            val saved = SavedWorkoutState(context)
            val interval = WorkoutCheckpointStore(File(directory, INTERVAL_CHECKPOINT_FILE)).load()
            return ResumeSources(
                single = SingleSessionCheckpoint.file(directory).load(),
                program = ProgramSessionCheckpointStore(File(directory, ProgramSessionCheckpointStore.FILE_NAME)).load(),
                savedProgramId = saved.getSavedProgramId(),
                savedProgramAtWallMillis = saved.getSavedAt(),
                interval = (interval as? WorkoutCheckpointStore.LoadResult.Found)?.checkpoint,
            )
        }
    }
}

/**
 * The workouts that can be continued, newest first. An entry whose exercise or
 * program no longer exists is left out, because its screen would not offer the
 * resume either; its saved data is not touched.
 */
fun resumableWorkouts(
    sources: ResumeSources,
    exerciseIds: Set<Long>,
    programIds: Set<Long>,
    intervalProgramIds: Set<Long>,
): List<ResumableWorkout> = buildList {
    sources.single?.takeIf { it.exerciseId in exerciseIds }?.let {
        add(ResumableWorkout.Single(it.exerciseId, it.savedAtWallMillis))
    }
    sources.program?.takeIf { it.programId in programIds }?.let {
        add(ResumableWorkout.Program(it.programId, savedByUser = false, savedAtWallMillis = it.savedAtWallMillis))
    }
    sources.savedProgramId?.takeIf { it in programIds }?.let {
        add(ResumableWorkout.Program(it, savedByUser = true, savedAtWallMillis = sources.savedProgramAtWallMillis))
    }
    val interval = sources.interval
    val intervalProgramId = interval?.interval?.programId
    if (interval != null && intervalProgramId != null && intervalProgramId in intervalProgramIds) {
        add(ResumableWorkout.Interval(intervalProgramId, interval.savedAtWallMillis))
    }
}.sortedByDescending { it.savedAtWallMillis }
