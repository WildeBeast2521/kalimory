package io.github.gonbei774.calisthenicsmemory.data.catalogue

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality

/** A step's text in the user's language when it is added; the library keeps it as written then. */
data class CatalogueStepText(val name: String, val description: String, val chainName: String)

sealed interface CatalogueAddResult {
    val exerciseId: Long

    /** A new library exercise, linked to the step. */
    data class Added(override val exerciseId: Long) : CatalogueAddResult

    /** The user already had an exercise of that name and kind; it is now linked instead of duplicated. */
    data class Linked(override val exerciseId: Long) : CatalogueAddResult

    /** The step was already in the library. */
    data class AlreadyInLibrary(override val exerciseId: Long) : CatalogueAddResult

    /** An exercise of that name and kind exists but is linked to another step; nothing changed. */
    data class NameTaken(override val exerciseId: Long) : CatalogueAddResult
}

/**
 * Adds catalogue steps to the user's library (ADR 0007). A step becomes an ordinary exercise: its
 * chain is the group, its difficulty the level, its move-on standard the target. The user can
 * change all of it afterwards; the link stays.
 */
object CatalogueLibrary {
    suspend fun add(database: AppDatabase, step: CatalogueStep, text: CatalogueStepText): CatalogueAddResult =
        database.withTransaction {
            val exercises = database.exerciseDao()
            exercises.getExerciseByCatalogId(step.id)?.let { return@withTransaction CatalogueAddResult.AlreadyInLibrary(it.id) }

            val type = if (step.kind == ExerciseKind.ISOMETRIC) "Isometric" else "Dynamic"
            exercises.findExerciseByNameIgnoringCase(text.name, type)?.let { existing ->
                return@withTransaction if (existing.catalogId == null) {
                    exercises.updateExercise(existing.copy(catalogId = step.id))
                    CatalogueAddResult.Linked(existing.id)
                } else {
                    CatalogueAddResult.NameTaken(existing.id)
                }
            }

            val groups = database.exerciseGroupDao()
            if (groups.getGroupByName(text.chainName) == null) {
                groups.insertGroup(ExerciseGroup(name = text.chainName, displayOrder = groups.getAllGroupsSync().size))
            }
            val id = exercises.insertExercise(
                Exercise(
                    name = text.name,
                    type = type,
                    group = text.chainName,
                    sortOrder = step.difficulty,
                    displayOrder = exercises.getMaxDisplayOrder() + 1,
                    laterality = if (step.laterality == Laterality.UNILATERAL) "Unilateral" else "Bilateral",
                    targetSets = step.moveOn.sets,
                    targetValue = step.moveOn.value,
                    description = text.description,
                    catalogId = step.id,
                )
            )
            CatalogueAddResult.Added(id)
        }
}
