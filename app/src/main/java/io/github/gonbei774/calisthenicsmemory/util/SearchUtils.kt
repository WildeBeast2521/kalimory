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

    /**
     * A program or interval a search found. [includes] names its exercises that the search matched
     * when its own name did not, so the row can say why it is there; it is empty otherwise.
     */
    data class Found<T>(val item: T, val includes: List<String> = emptyList())

    /**
     * Programs matching [query] by their name or the names of their exercises ([exerciseNames], by
     * program id); a program's own name ranks above a match on one of its exercises. A blank query
     * leaves the list as it is.
     */
    fun searchPrograms(programs: List<Program>, query: String, exerciseNames: (Long) -> List<String> = { emptyList() }): List<Found<Program>> =
        searchWithExercises(programs, query, { it.name }) { exerciseNames(it.id) }

    /** Interval programs, searched like [searchPrograms]. */
    fun searchIntervalPrograms(
        programs: List<IntervalProgram>,
        query: String,
        exerciseNames: (Long) -> List<String> = { emptyList() },
    ): List<Found<IntervalProgram>> =
        searchWithExercises(programs, query, { it.name }) { exerciseNames(it.id) }

    private fun <T> searchWithExercises(items: List<T>, query: String, name: (T) -> String, exercises: (T) -> List<String>): List<Found<T>> {
        val q = SearchQuery(query)
        if (q.isBlank) return items.map { Found(it) }
        return items.mapIndexedNotNull { index, item ->
            val names = exercises(item)
            q.rank(name(item), names)?.let { rank ->
                // Rank 2 or better is the program's own name; otherwise its exercises found it.
                val includes = if (rank <= 2) emptyList() else names.filter { q.mentions(it) }.distinct()
                Triple(rank, index, Found(item, includes))
            }
        }.sortedWith(compareBy({ it.first }, { it.second })).map { it.third }
    }
}
