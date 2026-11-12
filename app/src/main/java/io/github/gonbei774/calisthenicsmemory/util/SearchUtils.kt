package io.github.gonbei774.calisthenicsmemory.util

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

/**
 * The searches over the user's own exercises, groups and programs. Every one matches with
 * [SearchQuery]: case, accents and Arabic marks are ignored, every word of the query must appear,
 * and the best match comes first while equal matches keep the screen's own order (#278).
 */
object SearchUtils {

    /**
     * Exercises matching [query] by their name or their group's name; an exercise's own name ranks
     * above a match on its group. A blank query finds nothing.
     */
    fun searchExercises(exercises: List<Exercise>, query: String): List<Exercise> {
        val q = SearchQuery(query)
        if (q.isBlank) return emptyList()
        return exercises.rankedBy { q.rank(it.name, listOfNotNull(it.group)) }
    }

    /**
     * Groups and their exercises matching [query]. A group whose name matches keeps all its
     * exercises, in their usual order; otherwise it keeps the exercises that match, best first, and a
     * group with none is left out. [groupLabel] is the name the screen shows for a group, such as the
     * localized "Favorite" and "No group". A blank query leaves the list as it is.
     */
    fun searchHierarchicalExercises(
        hierarchicalData: List<TrainingViewModel.GroupWithExercises>,
        query: String,
        groupLabel: (String?) -> String? = { it },
    ): List<TrainingViewModel.GroupWithExercises> {
        val q = SearchQuery(query)
        if (q.isBlank) return hierarchicalData
        return hierarchicalData.mapNotNull { group ->
            val label = groupLabel(group.groupName)
            if (label != null && q.rank(label) != null) return@mapNotNull group
            val matching = group.exercises.rankedBy { q.rank(it.name, listOfNotNull(label)) }
            if (matching.isEmpty()) null else group.copy(exercises = matching)
        }
    }

    /** Programs matching [query] by name. A blank query leaves the list as it is. */
    fun searchPrograms(programs: List<Program>, query: String): List<Program> {
        val q = SearchQuery(query)
        if (q.isBlank) return programs
        return programs.rankedBy { q.rank(it.name) }
    }

    /** Interval programs matching [query] by name. A blank query leaves the list as it is. */
    fun searchIntervalPrograms(programs: List<IntervalProgram>, query: String): List<IntervalProgram> {
        val q = SearchQuery(query)
        if (q.isBlank) return programs
        return programs.rankedBy { q.rank(it.name) }
    }
}
