package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.app.Application
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel.GroupWithExercises
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The favourites card has no Rename/Delete menu: those acted on an internal name and did nothing. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FavoritesCardMenuTest {
    @get:Rule
    val compose = createComposeRule()

    private fun menus(groupName: String): Int {
        val vm = TrainingViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent {
            CalisthenicsMemoryTheme {
                ExpandableGroupCard(
                    group = GroupWithExercises(groupName, listOf(Exercise(id = 1, name = "Push-up", type = "Dynamic", isFavorite = true))),
                    isExpanded = false, onExpandToggle = {}, onGroupMenuClick = {},
                    onExerciseEdit = {}, onExerciseDelete = {}, viewModel = vm,
                )
            }
        }
        return compose.onAllNodes(hasContentDescription("Menu"), useUnmergedTree = true).fetchSemanticsNodes().size
    }

    @Test fun `favourites have no group menu`() = assertEquals(0, menus(TrainingViewModel.FAVORITE_GROUP_KEY))

    @Test fun `a real group keeps its menu`() = assertEquals(1, menus("Push"))
}
