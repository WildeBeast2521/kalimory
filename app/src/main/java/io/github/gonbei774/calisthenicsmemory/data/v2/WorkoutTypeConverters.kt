package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.TypeConverter

/** Persists v2 enums as their stable codes. An unknown code fails loudly rather than guessing. */
class WorkoutTypeConverters {
    @TypeConverter fun sessionStatusToCode(value: WorkoutSessionStatus): String = value.code
    @TypeConverter fun codeToSessionStatus(code: String): WorkoutSessionStatus = codeOf(code)

    @TypeConverter fun sourceTypeToCode(value: WorkoutSourceType): String = value.code
    @TypeConverter fun codeToSourceType(code: String): WorkoutSourceType = codeOf(code)

    @TypeConverter fun setStatusToCode(value: SetEntryStatus): String = value.code
    @TypeConverter fun codeToSetStatus(code: String): SetEntryStatus = codeOf(code)

    @TypeConverter fun bodySideToCode(value: BodySide): String = value.code
    @TypeConverter fun codeToBodySide(code: String): BodySide = codeOf(code)

    @TypeConverter fun exerciseKindToCode(value: ExerciseKind): String = value.code
    @TypeConverter fun codeToExerciseKind(code: String): ExerciseKind = codeOf(code)

    @TypeConverter fun lateralityToCode(value: Laterality): String = value.code
    @TypeConverter fun codeToLaterality(code: String): Laterality = codeOf(code)

    @TypeConverter fun timePrecisionToCode(value: TimePrecision): String = value.code
    @TypeConverter fun codeToTimePrecision(code: String): TimePrecision = codeOf(code)
}
