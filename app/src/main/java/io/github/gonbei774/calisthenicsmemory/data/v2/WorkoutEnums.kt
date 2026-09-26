package io.github.gonbei774.calisthenicsmemory.data.v2

/**
 * v2 workout enums. Each value persists as its [code], never its ordinal. Codes are
 * part of the database format: never rename or reuse one; add new values instead.
 */
interface StableCode {
    val code: String
}

enum class WorkoutSessionStatus(override val code: String) : StableCode {
    PLANNED("PLANNED"),
    ACTIVE("ACTIVE"),
    PAUSED("PAUSED"),
    COMPLETED("COMPLETED"),
    ABANDONED("ABANDONED"),
}

enum class WorkoutSourceType(override val code: String) : StableCode {
    AD_HOC("AD_HOC"),
    PROGRAM_TEMPLATE("PROGRAM_TEMPLATE"),
    INTERVAL_TEMPLATE("INTERVAL_TEMPLATE"),
    LEGACY_IMPORT("LEGACY_IMPORT"),
}

enum class SetEntryStatus(override val code: String) : StableCode {
    PENDING("PENDING"),
    COMPLETED("COMPLETED"),
    SKIPPED("SKIPPED"),
}

enum class BodySide(override val code: String) : StableCode {
    BILATERAL("BILATERAL"),
    RIGHT("RIGHT"),
    LEFT("LEFT"),
}

enum class ExerciseKind(override val code: String) : StableCode {
    DYNAMIC("DYNAMIC"),
    ISOMETRIC("ISOMETRIC"),
}

enum class Laterality(override val code: String) : StableCode {
    BILATERAL("BILATERAL"),
    UNILATERAL("UNILATERAL"),
}

/**
 * How precise a stored timestamp is. Legacy records only have a local date and a
 * minute, so times derived from them are [MINUTE]; they must not be shown or
 * compared as if they were exact observations.
 */
enum class TimePrecision(override val code: String) : StableCode {
    EXACT("EXACT"),
    MINUTE("MINUTE"),
}

inline fun <reified E> codeOf(code: String): E where E : Enum<E>, E : StableCode =
    enumValues<E>().firstOrNull { it.code == code }
        ?: throw IllegalArgumentException("Unknown ${E::class.simpleName} code: $code")
