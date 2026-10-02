package io.github.gonbei774.calisthenicsmemory.data.catalogue

import androidx.annotation.StringRes
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality

/**
 * The built-in exercise catalogue (ADR 0006): chains of steps from easier to harder (ADR 0005).
 * It is compiled into the app; the database stores only the user's links to it (ADR 0007).
 */

/** Muscles, as stable codes shared by the catalogue, the muscle map and backups (ADR 0006). */
enum class Muscle(val code: String) {
    ABDOMINALS("abdominals"),
    ABDUCTORS("abductors"),
    ADDUCTORS("adductors"),
    BICEPS("biceps"),
    CALVES("calves"),
    CHEST("chest"),
    FOREARMS("forearms"),
    GLUTES("glutes"),
    HAMSTRINGS("hamstrings"),
    LATS("lats"),
    LOWER_BACK("lower back"),
    MIDDLE_BACK("middle back"),
    NECK("neck"),
    QUADRICEPS("quadriceps"),
    SHOULDERS("shoulders"),
    TRAPS("traps"),
    TRICEPS("triceps"),
}

/** What a step needs besides the floor. Nothing listed means floor and bodyweight only. */
enum class Equipment {
    PULL_UP_BAR,
    PARALLEL_BARS,
    /** A bench, chair, step or box. */
    BENCH,
    WALL,
    /** A waist-high bar or sturdy table edge to row from. */
    LOW_BAR,
    RESISTANCE_BAND,
    /** A towel or sliders on a smooth floor. */
    SLIDER,
    /** Something sturdy to hold the ankles down. */
    ANCHOR,
    /** A vertical pole, a post or wall bars to grip. */
    POLE,
}

/** The movement a chain trains; the same pattern rests about 48 hours between sessions (ADR 0005). */
enum class MovementPattern {
    HORIZONTAL_PUSH,
    VERTICAL_PUSH,
    HORIZONTAL_PULL,
    VERTICAL_PULL,
    SQUAT,
    HINGE,
    CORE_ANTI_EXTENSION,
    CORE_FLEXION,
    CORE_LATERAL,
}

/** Sets of reps, or of seconds for a hold. */
data class Standard(val sets: Int, val value: Int)

data class CatalogueStep(
    /** Stable id, never reused or renamed once released (ADR 0006), such as "push.incline". */
    val id: String,
    val chainId: String,
    @StringRes val name: Int,
    @StringRes val description: Int,
    /** Form cues, one per line. */
    @StringRes val cues: Int,
    val kind: ExerciseKind,
    val laterality: Laterality,
    /** 1 (easiest) to 10, one scale across all chains. */
    val difficulty: Int,
    /** Where the step starts (ADR 0005). */
    val working: Standard,
    /** Where the step is mastered and the next one is suggested. */
    val moveOn: Standard,
    val primaryMuscles: Set<Muscle>,
    val secondaryMuscles: Set<Muscle>,
    val equipment: Set<Equipment>,
    /** Steps in other chains that help first. Guidance only: nothing is ever locked (ADR 0005). */
    val prerequisites: Set<String> = emptySet(),
    /** Kept so old links still resolve, but no longer offered. */
    val retired: Boolean = false,
)

data class CatalogueChain(
    val id: String,
    @StringRes val name: Int,
    val pattern: MovementPattern,
    /** Easiest first. */
    val steps: List<CatalogueStep>,
)
