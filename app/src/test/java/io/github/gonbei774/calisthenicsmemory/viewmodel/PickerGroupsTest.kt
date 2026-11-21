package io.github.gonbei774.calisthenicsmemory.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercise pickers open every group and remember only the ones closed, apart from the Library's. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PickerGroupsTest {
    private val vm = TrainingViewModel(ApplicationProvider.getApplicationContext<Application>())

    @Test fun `no group is collapsed at first`() {
        assertTrue(vm.collapsedPickerGroups.value.isEmpty())
    }

    @Test fun `toggling closes a group and again opens it`() {
        vm.togglePickerGroup("Push")
        assertEquals(setOf("Push"), vm.collapsedPickerGroups.value)
        vm.togglePickerGroup("Push")
        assertTrue(vm.collapsedPickerGroups.value.isEmpty())
    }

    @Test fun `the Library's open groups do not change a picker`() {
        vm.toggleGroupExpansion("Push")
        assertTrue(vm.collapsedPickerGroups.value.isEmpty())
        assertEquals(setOf("Push"), vm.expandedGroups.value)
    }
}
