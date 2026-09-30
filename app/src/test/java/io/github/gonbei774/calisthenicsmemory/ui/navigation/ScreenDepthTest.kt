package io.github.gonbei774.calisthenicsmemory.ui.navigation

import io.github.gonbei774.calisthenicsmemory.Screen
import io.github.gonbei774.calisthenicsmemory.workoutExit
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every in-app back destination must be shallower than the screen it leaves, so it slides back. */
class ScreenDepthTest {
    private fun assertBack(from: Screen, to: Screen) =
        assertTrue("$from -> $to should slide back", to.depth() < from.depth())

    @Test fun `back destinations are shallower`() {
        assertBack(Screen.ToDo, Screen.Home)
        assertBack(Screen.Settings, Screen.Home)
        assertBack(Screen.Licenses, Screen.Settings)
        assertBack(Screen.Backup, Screen.Settings)
        assertBack(Screen.CsvDataManagement, Screen.Settings)
        assertBack(Screen.ShareHub, Screen.Settings)
        assertBack(Screen.CommunityShareExport, Screen.ShareHub)
        assertBack(Screen.ProgramEdit(1), Screen.ProgramList)
        assertBack(Screen.IntervalEdit(null), Screen.IntervalList)
        assertBack(Screen.ProgramExecution(1), Screen.ProgramList)
        assertBack(Screen.ProgramExecution(1, fromToDo = true), Screen.ToDo)
        assertBack(Screen.ProgramExecution(1, fromToday = true), Screen.Home)
        assertBack(Screen.IntervalExecution(1), Screen.IntervalList)
        assertBack(Screen.IntervalExecution(1, fromToDo = true), Screen.ToDo)
        assertBack(Screen.Record(fromToDo = true), Screen.ToDo)
        assertBack(Screen.Record(), Screen.Home)
        assertBack(Screen.Workout(fromToDo = true), Screen.ToDo)
        assertBack(Screen.Workout(fromToDo = true, fromToday = true), Screen.Home)
    }

    @Test fun `the workout summary slides back to wherever the workout returns`() {
        listOf(
            Screen.Workout(), Screen.Workout(fromToDo = true),
            Screen.ProgramExecution(1), Screen.ProgramExecution(1, fromToDo = true), Screen.ProgramExecution(1, fromToday = true),
            Screen.IntervalExecution(1), Screen.IntervalExecution(1, fromToDo = true), Screen.IntervalExecution(1, fromToday = true),
        ).forEach { workout -> assertBack(Screen.WorkoutSummary(1), workout.workoutExit()!!) }
    }
}
