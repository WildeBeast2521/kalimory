package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel.Companion.FAVORITE_GROUP_KEY
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryHierarchyTest {
    private fun exercise(id: Long, name: String, group: String? = null, order: Int = 0, favorite: Boolean = false) =
        Exercise(id = id, name = name, type = "Dynamic", group = group, displayOrder = order, isFavorite = favorite)

    private val push = ExerciseGroup(id = 1, name = "Push", displayOrder = 0)
    private val legs = ExerciseGroup(id = 2, name = "Legs", displayOrder = 1)

    @Test fun `groups keep their given order, empty ones included, with exercises in display order`() {
        val sections = libraryHierarchy(
            listOf(legs, push),
            listOf(exercise(1, "Dip", "Push", order = 1), exercise(2, "Push-up", "Push", order = 0)),
            expanded = setOf("Push"),
        )
        assertEquals(listOf("Legs", "Push"), sections.map { it.groupName })
        assertEquals(emptyList<String>(), sections[0].exercises.map { it.name })
        assertEquals(listOf("Push-up", "Dip"), sections[1].exercises.map { it.name })
        assertEquals(listOf(false, true), sections.map { it.isExpanded })
    }

    @Test fun `favourites come first and ungrouped exercises last, each only when there are some`() {
        val none = libraryHierarchy(listOf(push), listOf(exercise(1, "Push-up", "Push")), emptySet())
        assertEquals(listOf("Push"), none.map { it.groupName })

        val all = libraryHierarchy(
            listOf(push),
            listOf(exercise(1, "Push-up", "Push", favorite = true), exercise(2, "Plank", order = 1), exercise(3, "Burpee", order = 0)),
            expanded = setOf("ungrouped", FAVORITE_GROUP_KEY),
        )
        assertEquals(listOf(FAVORITE_GROUP_KEY, "Push", null), all.map { it.groupName })
        assertEquals(listOf("Push-up"), all.first().exercises.map { it.name })
        assertEquals(listOf("Burpee", "Plank"), all.last().exercises.map { it.name })
        assertEquals(listOf(true, false, true), all.map { it.isExpanded })
    }
}
