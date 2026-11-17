package io.github.gonbei774.calisthenicsmemory.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** Exercise editor rules: the level is never changed by itself, and half a target is never saved. */
class ExerciseEditorRulesTest {
    @Test fun `an untouched level is kept, including not set`() {
        assertEquals(7, levelToSave(changed = false, selected = 5, stored = 7))
        // A stored 0 used to come back as 1, a value nobody chose.
        assertEquals(0, levelToSave(changed = false, selected = 5, stored = 0))
    }

    @Test fun `a moved slider saves the new level`() {
        assertEquals(3, levelToSave(changed = true, selected = 3, stored = 7))
    }

    @Test fun `half a target can't be saved, both or neither can`() {
        assertEquals(true, isTargetHalfFilled(true, "3", ""))
        assertEquals(true, isTargetHalfFilled(true, "", "10"))
        assertEquals(false, isTargetHalfFilled(true, "3", "10"))
        assertEquals(false, isTargetHalfFilled(true, "", ""))
        assertEquals(false, isTargetHalfFilled(false, "3", ""))
    }
}
