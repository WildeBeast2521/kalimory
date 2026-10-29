package io.github.gonbei774.calisthenicsmemory.viewmodel

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel.Companion.FAVORITE_GROUP_KEY
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel.GroupWithExercises

/**
 * The library list: favourites first (only when there are any), then every group in its order,
 * empty ones too, then the exercises without a group. Exercises keep their display order within
 * each; [expanded] holds the open sections ("ungrouped" for the last).
 */
internal fun libraryHierarchy(
    groups: List<ExerciseGroup>,
    exercises: List<Exercise>,
    expanded: Set<String>,
): List<GroupWithExercises> {
    val favorites = exercises.filter { it.isFavorite }.sortedBy { it.displayOrder }
    val favoriteSection = if (favorites.isEmpty()) emptyList() else listOf(
        GroupWithExercises(FAVORITE_GROUP_KEY, favorites, isExpanded = FAVORITE_GROUP_KEY in expanded)
    )
    val groupSections = groups.map { group ->
        GroupWithExercises(
            groupName = group.name,
            exercises = exercises.filter { it.group == group.name }.sortedBy { it.displayOrder },
            isExpanded = group.name in expanded,
        )
    }
    val ungrouped = exercises.filter { it.group == null }.sortedBy { it.displayOrder }
    val ungroupedSection = if (ungrouped.isEmpty()) emptyList() else listOf(
        GroupWithExercises(groupName = null, exercises = ungrouped, isExpanded = "ungrouped" in expanded)
    )
    return favoriteSection + groupSections + ungroupedSection
}

/** The library's stored order: moving an exercise within its group, and saving the group order. */
class LibraryActions(private val database: AppDatabase) {
    private val exerciseDao = database.exerciseDao()
    private val groupDao = database.exerciseGroupDao()

    /**
     * Moves the exercise at [fromIndex] to [toIndex] within [groupName] (null for ungrouped) and
     * renumbers the group. Read and rewrite are one transaction, so quick moves never interleave.
     * Favourites are a view of other groups and are not reordered; out-of-range moves change nothing.
     */
    suspend fun moveExercise(groupName: String?, fromIndex: Int, toIndex: Int) {
        if (groupName == FAVORITE_GROUP_KEY) return
        database.withTransaction {
            val exercises = (if (groupName == null) exerciseDao.getUngroupedExercises() else exerciseDao.getExercisesByGroup(groupName))
                .toMutableList()
            if (fromIndex !in exercises.indices || toIndex !in exercises.indices) return@withTransaction
            exercises.add(toIndex, exercises.removeAt(fromIndex))
            exercises.forEachIndexed { index, exercise -> exerciseDao.updateExercise(exercise.copy(displayOrder = index)) }
        }
    }

    /** Stores [order] as the group order in one transaction, so the list never shows it half applied. */
    suspend fun saveGroupOrder(order: List<ExerciseGroup>) {
        database.withTransaction {
            order.forEachIndexed { index, group -> groupDao.updateGroup(group.copy(displayOrder = index)) }
        }
    }
}
