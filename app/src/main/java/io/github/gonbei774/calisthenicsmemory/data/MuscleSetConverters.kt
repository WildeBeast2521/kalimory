package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.TypeConverter
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle

/** Persists a set of muscles as comma-joined stable codes in enum order, "" for none. An unknown code fails loudly. */
@JvmSuppressWildcards
class MuscleSetConverters {
    @TypeConverter fun musclesToText(value: Set<Muscle>): String =
        Muscle.entries.filter { it in value }.joinToString(",") { it.code }

    @TypeConverter fun textToMuscles(text: String): Set<Muscle> =
        if (text.isEmpty()) emptySet() else text.split(",").mapTo(linkedSetOf()) { muscleOfCode(it) }
}

/** The muscle with this stable [code]; throws for a code this version does not know. */
fun muscleOfCode(code: String): Muscle =
    Muscle.entries.firstOrNull { it.code == code } ?: throw IllegalArgumentException("Unknown muscle code: $code")
