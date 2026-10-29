package io.github.gonbei774.calisthenicsmemory.viewmodel

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.TodoTask

/**
 * Deleting, copying and reordering programs and interval programs. Each runs in one transaction,
 * so a failure leaves no half-made copy, no to-do pointing at a deleted program, and no order
 * half rewritten.
 */
class ProgramActions(private val database: AppDatabase) {
    private val programDao = database.programDao()
    private val programExerciseDao = database.programExerciseDao()
    private val programLoopDao = database.programLoopDao()
    private val intervalProgramDao = database.intervalProgramDao()
    private val intervalProgramExerciseDao = database.intervalProgramExerciseDao()
    private val todoTaskDao = database.todoTaskDao()

    /** Deletes a program (its exercises and loops cascade) and its to-do. */
    suspend fun deleteProgram(programId: Long) = database.withTransaction {
        programDao.deleteById(programId)
        todoTaskDao.deleteByReference(TodoTask.TYPE_PROGRAM, programId)
    }

    /** Deletes an interval program (its exercises cascade) and its to-do. */
    suspend fun deleteIntervalProgram(programId: Long) = database.withTransaction {
        intervalProgramDao.deleteById(programId)
        todoTaskDao.deleteByReference(TodoTask.TYPE_INTERVAL, programId)
    }

    /**
     * Copies a program as "<name> [copySuffix]" with its loops and exercises, each exercise in the
     * copy's matching loop. Returns the copy's id, or null when the program no longer exists.
     */
    suspend fun duplicateProgram(programId: Long, copySuffix: String): Long? = database.withTransaction {
        val source = programDao.getProgramById(programId) ?: return@withTransaction null
        val newProgramId = programDao.insert(Program(name = "${source.name} $copySuffix"))
        val loopIds = programLoopDao.getLoopsForProgramSync(programId).associate { loop ->
            loop.id to programLoopDao.insert(
                ProgramLoop(
                    programId = newProgramId,
                    sortOrder = loop.sortOrder,
                    rounds = loop.rounds,
                    restBetweenRounds = loop.restBetweenRounds,
                )
            )
        }
        programExerciseDao.getExercisesForProgramSync(programId).forEach { pe ->
            programExerciseDao.insert(
                ProgramExercise(
                    programId = newProgramId,
                    exerciseId = pe.exerciseId,
                    sortOrder = pe.sortOrder,
                    sets = pe.sets,
                    targetValue = pe.targetValue,
                    intervalSeconds = pe.intervalSeconds,
                    loopId = pe.loopId?.let(loopIds::get),
                )
            )
        }
        newProgramId
    }

    /** Copies an interval program as "<name> [copySuffix]" with its exercises; null when it no longer exists. */
    suspend fun duplicateIntervalProgram(programId: Long, copySuffix: String): Long? = database.withTransaction {
        val source = intervalProgramDao.getProgramById(programId) ?: return@withTransaction null
        val newId = intervalProgramDao.insert(
            IntervalProgram(
                name = "${source.name} $copySuffix",
                workSeconds = source.workSeconds,
                restSeconds = source.restSeconds,
                rounds = source.rounds,
                roundRestSeconds = source.roundRestSeconds,
            )
        )
        intervalProgramExerciseDao.getExercisesForProgramSync(programId).forEach { exercise ->
            intervalProgramExerciseDao.insert(
                IntervalProgramExercise(programId = newId, exerciseId = exercise.exerciseId, sortOrder = exercise.sortOrder)
            )
        }
        newId
    }

    /** Moves the program's exercise at [fromIndex] to [toIndex]; out-of-range indices change nothing. */
    suspend fun moveProgramExercise(programId: Long, fromIndex: Int, toIndex: Int) = database.withTransaction {
        val exercises = programExerciseDao.getExercisesForProgramSync(programId).toMutableList()
        if (fromIndex !in exercises.indices || toIndex !in exercises.indices) return@withTransaction
        exercises.add(toIndex, exercises.removeAt(fromIndex))
        programExerciseDao.reorderExercises(exercises.map { it.id })
    }
}
