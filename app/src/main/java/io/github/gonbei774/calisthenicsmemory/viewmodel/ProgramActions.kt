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
/**
 * What the program editor saves: the program as stored when the editor opened ([program], null for
 * a new one) and its loops and exercises then ([originalLoops], [originalExercises]), against what
 * the editor now holds. New loops and exercises carry temporary ids that are not in the originals.
 */
data class ProgramDraft(
    val program: Program?,
    val name: String,
    val originalLoops: List<ProgramLoop>,
    val loops: List<ProgramLoop>,
    val originalExercises: List<ProgramExercise>,
    val exercises: List<ProgramExercise>,
)

/** What the interval editor saves, against the interval program as it was when the editor opened. */
data class IntervalProgramDraft(
    val program: IntervalProgram?,
    val name: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val roundRestSeconds: Int,
    val originalExercises: List<IntervalProgramExercise>,
    val exercises: List<IntervalProgramExercise>,
)

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

    /**
     * Saves the program editor's [draft] over program [programId], or as a new program when it is
     * null, in one transaction: loops first (removed, added, changed), then exercises with their
     * loops mapped to the stored ones. A failure part-way saves nothing.
     */
    suspend fun saveProgram(programId: Long?, draft: ProgramDraft) = database.withTransaction {
        if (programId != null) {
            draft.program?.let { programDao.update(it.copy(name = draft.name)) }

            val originalLoopIds = draft.originalLoops.map { it.id }.toSet()
            val loopIds = mutableMapOf<Long, Long>()
            // Removed loops go first; their exercises cascade with them.
            (originalLoopIds - draft.loops.map { it.id }.toSet()).forEach { id ->
                draft.originalLoops.find { it.id == id }?.let { programLoopDao.delete(it) }
            }
            draft.loops.filter { it.id !in originalLoopIds }.forEach { loop ->
                loopIds[loop.id] = programLoopDao.insert(
                    ProgramLoop(
                        programId = programId,
                        sortOrder = programLoopDao.getNextSortOrder(programId),
                        rounds = loop.rounds,
                        restBetweenRounds = loop.restBetweenRounds,
                    )
                )
            }
            draft.loops.filter { it.id in originalLoopIds }.forEach { loop ->
                if (draft.originalLoops.find { it.id == loop.id }.let { it != null && it != loop }) programLoopDao.update(loop)
                loopIds[loop.id] = loop.id
            }

            val originalIds = draft.originalExercises.map { it.id }.toSet()
            (originalIds - draft.exercises.map { it.id }.toSet()).forEach { id ->
                draft.originalExercises.find { it.id == id }?.let { programExerciseDao.delete(it) }
            }
            draft.exercises.filter { it.id !in originalIds }.forEach { pe ->
                programExerciseDao.insert(pe.copy(id = 0, programId = programId, loopId = pe.loopId?.let(loopIds::get)))
            }
            draft.exercises.filter { it.id in originalIds }.forEach { pe ->
                val updated = pe.copy(loopId = pe.loopId?.let(loopIds::get))
                if (draft.originalExercises.find { it.id == pe.id }.let { it != null && it != updated }) programExerciseDao.update(updated)
            }
        } else {
            val newProgramId = programDao.insert(Program(name = draft.name))
            val loopIds = draft.loops.associate { loop ->
                loop.id to programLoopDao.insert(
                    ProgramLoop(
                        programId = newProgramId,
                        sortOrder = programLoopDao.getNextSortOrder(newProgramId),
                        rounds = loop.rounds,
                        restBetweenRounds = loop.restBetweenRounds,
                    )
                )
            }
            draft.exercises.forEach { pe ->
                programExerciseDao.insert(pe.copy(id = 0, programId = newProgramId, loopId = pe.loopId?.let(loopIds::get)))
            }
        }
    }

    /** Saves the interval editor's [draft] over [programId], or as a new one when null, in one transaction. */
    suspend fun saveIntervalProgram(programId: Long?, draft: IntervalProgramDraft) = database.withTransaction {
        if (programId != null) {
            draft.program?.let {
                intervalProgramDao.update(
                    it.copy(
                        name = draft.name,
                        workSeconds = draft.workSeconds,
                        restSeconds = draft.restSeconds,
                        rounds = draft.rounds,
                        roundRestSeconds = draft.roundRestSeconds,
                    )
                )
            }
            val originalIds = draft.originalExercises.map { it.id }.toSet()
            (originalIds - draft.exercises.map { it.id }.toSet()).forEach { id ->
                draft.originalExercises.find { it.id == id }?.let { intervalProgramExerciseDao.delete(it) }
            }
            draft.exercises.filter { it.id !in originalIds }.forEach { pe ->
                intervalProgramExerciseDao.insert(IntervalProgramExercise(programId = programId, exerciseId = pe.exerciseId, sortOrder = pe.sortOrder))
            }
            draft.exercises.filter { it.id in originalIds }.forEach { pe ->
                if (draft.originalExercises.find { it.id == pe.id }.let { it != null && it != pe }) intervalProgramExerciseDao.update(pe)
            }
        } else {
            val newId = intervalProgramDao.insert(
                IntervalProgram(
                    name = draft.name,
                    workSeconds = draft.workSeconds,
                    restSeconds = draft.restSeconds,
                    rounds = draft.rounds,
                    roundRestSeconds = draft.roundRestSeconds,
                )
            )
            draft.exercises.forEach { pe ->
                intervalProgramExerciseDao.insert(IntervalProgramExercise(programId = newId, exerciseId = pe.exerciseId, sortOrder = pe.sortOrder))
            }
        }
    }
}
