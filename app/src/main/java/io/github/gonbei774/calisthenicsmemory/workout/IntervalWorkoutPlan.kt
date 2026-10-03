package io.github.gonbei774.calisthenicsmemory.workout

import kotlinx.serialization.Serializable

enum class IntervalStepType { PREPARE, WORK, REST, ROUND_REST }

/** One interval step. [round] is 1-based; [exerciseIndex] is the exercise that is running or just finished. */
data class IntervalStep(val type: IntervalStepType, val round: Int, val exerciseIndex: Int)

/**
 * Turns an interval program into countdown [WorkoutStep]s, in the order the interval
 * screen has always used: a 5-second prepare; then each round's work periods,
 * separated by rests; then a round rest between rounds. Rests and round rests of
 * zero seconds are left out.
 */
object IntervalWorkoutPlan {
    const val PREPARE_MILLIS = 5_000L

    fun build(workSeconds: Int, restSeconds: Int, rounds: Int, roundRestSeconds: Int, exerciseCount: Int): List<WorkoutStep> =
        buildList {
            add(WorkoutStep("prepare", StepKind.Countdown(PREPARE_MILLIS)))
            for (round in 1..rounds) {
                for (index in 0 until exerciseCount) {
                    if (workSeconds > 0) add(WorkoutStep("work:$round:$index", StepKind.Countdown(workSeconds * 1_000L)))
                    val isLastExercise = index == exerciseCount - 1
                    if (!isLastExercise && restSeconds > 0) {
                        add(WorkoutStep("rest:$round:$index", StepKind.Countdown(restSeconds * 1_000L)))
                    }
                }
                if (round < rounds && roundRestSeconds > 0) {
                    add(WorkoutStep("roundRest:$round", StepKind.Countdown(roundRestSeconds * 1_000L)))
                }
            }
        }

    fun describe(step: WorkoutStep, exerciseCount: Int): IntervalStep {
        val parts = step.id.split(":")
        return when (parts[0]) {
            "prepare" -> IntervalStep(IntervalStepType.PREPARE, 1, 0)
            "work" -> IntervalStep(IntervalStepType.WORK, parts[1].toInt(), parts[2].toInt())
            "rest" -> IntervalStep(IntervalStepType.REST, parts[1].toInt(), parts[2].toInt())
            "roundRest" -> IntervalStep(IntervalStepType.ROUND_REST, parts[1].toInt(), exerciseCount - 1)
            else -> throw IllegalArgumentException("Not an interval step: ${step.id}")
        }
    }

    /**
     * [progress] with a fully finished last round counted as a round, so "1 round + 1 of 1
     * exercises" reads as "2 rounds", the way history shows the same workout.
     */
    fun normalized(progress: Pair<Int, Int>, exerciseCount: Int): Pair<Int, Int> {
        val (rounds, exercises) = progress
        return if (exerciseCount > 0 && exercises >= exerciseCount) (rounds + exercises / exerciseCount) to (exercises % exerciseCount)
        else progress
    }

    /**
     * Completed rounds and completed exercises in the last round when the user stops
     * during [step]. These are the same rules the screen used before.
     */
    fun progressWhenStopped(step: IntervalStep, exerciseCount: Int): Pair<Int, Int> = when (step.type) {
        IntervalStepType.PREPARE -> 0 to 0
        IntervalStepType.WORK -> (step.round - 1) to step.exerciseIndex
        IntervalStepType.REST -> (step.round - 1) to (step.exerciseIndex + 1)
        IntervalStepType.ROUND_REST -> step.round to exerciseCount
    }
}

/**
 * The interval program as it was when the workout started. A resumed workout uses
 * this snapshot, so later edits to the program do not change it or its record.
 */
@Serializable
data class IntervalSessionContext(
    val programId: Long,
    val programName: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val roundRestSeconds: Int,
    val exercises: List<IntervalExerciseSnapshot>,
    /** When the workout started (wall clock); absent in checkpoints written before it was recorded. */
    val startedAtWallMillis: Long? = null,
)

@Serializable
data class IntervalExerciseSnapshot(val exerciseId: Long, val name: String, val description: String?)
