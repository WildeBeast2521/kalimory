package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel.GroupWithExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The searches over the user's exercises, groups and programs (#278). */
class SearchUtilsTest {

    private fun ex(id: Long, name: String, group: String? = null) = Exercise(id = id, name = name, type = "Dynamic", group = group)

    private val exercises = listOf(
        ex(1, "Push-up", "Upper body"),
        ex(2, "Pull-up", "Upper body"),
        ex(3, "Push-up variation", "Upper body"),
        ex(4, "Diamond push-up", "Upper body"),
        ex(5, "Squat", "Legs"),
        ex(6, "Push press"),
    )

    private fun names(list: List<Exercise>) = list.map { it.name }

    @Test fun `a blank query finds nothing`() {
        assertTrue(SearchUtils.searchExercises(exercises, "").isEmpty())
        assertTrue(SearchUtils.searchExercises(exercises, "   ").isEmpty())
    }

    @Test fun `the name itself first, then names starting with the query, then the rest`() {
        assertEquals(listOf("Push-up", "Push-up variation", "Diamond push-up"), names(SearchUtils.searchExercises(exercises, "push-up")))
    }

    @Test fun `equal matches keep the list's own order, not alphabetical`() {
        assertEquals(listOf("Push-up", "Push-up variation", "Push press", "Diamond push-up"), names(SearchUtils.searchExercises(exercises, "push")))
    }

    @Test fun `case, accents and Arabic marks do not matter`() {
        val list = listOf(ex(1, "Pompe déclinée"), ex(2, "التعلّق"), ex(3, "Liegestütz"))
        assertEquals(listOf("Pompe déclinée"), names(SearchUtils.searchExercises(list, "POMPE DECLINEE")))
        assertEquals(listOf("التعلّق"), names(SearchUtils.searchExercises(list, "التعلق")))
        assertEquals(listOf("Liegestütz"), names(SearchUtils.searchExercises(list, "liegestutz")))
    }

    @Test fun `every word must match, in any order`() {
        assertEquals(listOf("Pull-up"), names(SearchUtils.searchExercises(exercises, "up pull")))
        assertTrue(SearchUtils.searchExercises(exercises, "pull squat").isEmpty())
    }

    @Test fun `Japanese and Chinese match any part of a name`() {
        val list = listOf(ex(1, "ダイヤモンド腕立て伏せ"), ex(2, "钻石俯卧撑"))
        assertEquals(listOf("ダイヤモンド腕立て伏せ"), names(SearchUtils.searchExercises(list, "腕立て")))
        assertEquals(listOf("钻石俯卧撑"), names(SearchUtils.searchExercises(list, "卧撑")))
    }

    @Test fun `a picker finds exercises by their group's name, below their own name matches`() {
        val list = listOf(ex(1, "Squat", "Legs"), ex(2, "Leg raise", "Core"), ex(3, "Lunge", "Legs"))
        // Leg raise by its own name; Squat and Lunge by their group "Legs".
        assertEquals(listOf("Leg raise", "Squat", "Lunge"), names(SearchUtils.searchExercises(list, "leg")))
    }

    private val groups = listOf(
        GroupWithExercises(TrainingViewModel.FAVORITE_GROUP_KEY, listOf(ex(5, "Squat", "Legs"))),
        GroupWithExercises("Upper body", listOf(ex(1, "Push-up", "Upper body"), ex(2, "Pull-up", "Upper body"), ex(4, "Diamond push-up", "Upper body"))),
        GroupWithExercises("Legs", listOf(ex(5, "Squat", "Legs"))),
        GroupWithExercises("Empty", emptyList()),
        GroupWithExercises(null, listOf(ex(6, "Push press"))),
    )
    private val label: (String?) -> String? = {
        when (it) {
            TrainingViewModel.FAVORITE_GROUP_KEY -> "Favorite"
            null -> "No group"
            else -> it
        }
    }

    private fun search(query: String) = SearchUtils.searchHierarchicalExercises(groups, query, label)
        .map { group -> (group.groupName ?: "-") to names(group.exercises) }

    @Test fun `a group whose name matches shows with all its exercises, in order`() {
        assertEquals(listOf("Upper body" to listOf("Push-up", "Pull-up", "Diamond push-up")), search("upper"))
    }

    @Test fun `an empty group is found by its name`() {
        assertEquals(listOf("Empty" to emptyList<String>()), search("empty"))
    }

    @Test fun `Favorite and No group match the names the screen shows`() {
        assertEquals(listOf(TrainingViewModel.FAVORITE_GROUP_KEY to listOf("Squat")), search("favorite"))
        assertEquals(listOf("-" to listOf("Push press")), search("no group"))
    }

    @Test fun `otherwise a group keeps only its matching exercises, best first`() {
        assertEquals(
            listOf("Upper body" to listOf("Push-up", "Diamond push-up"), "-" to listOf("Push press")),
            search("push"),
        )
    }

    @Test fun `words can match across a group's name and an exercise's`() {
        assertEquals(listOf("Upper body" to listOf("Pull-up")), search("upper pull"))
    }

    @Test fun `a blank query leaves the groups as they are`() {
        assertEquals(groups, SearchUtils.searchHierarchicalExercises(groups, " ", label))
    }

    @Test fun `programs match by name, ties keep their order, blank keeps all`() {
        val programs = listOf(Program(id = 1, name = "Upper body"), Program(id = 2, name = "Full body"), Program(id = 3, name = "Legs"))
        assertEquals(listOf("Upper body", "Full body"), SearchUtils.searchPrograms(programs, "body").map { it.name })
        assertEquals(listOf("Full body"), SearchUtils.searchPrograms(programs, "FULL").map { it.name })
        assertEquals(programs, SearchUtils.searchPrograms(programs, ""))
    }
}
