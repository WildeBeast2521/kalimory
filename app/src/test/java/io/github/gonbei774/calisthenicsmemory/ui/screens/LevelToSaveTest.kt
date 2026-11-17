package io.github.gonbei774.calisthenicsmemory.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** Editing an exercise never changes its level by itself (B2). */
class LevelToSaveTest {
    @Test fun `an untouched level is kept, including not set`() {
        assertEquals(7, levelToSave(changed = false, selected = 5, stored = 7))
        // A stored 0 used to come back as 1, a value nobody chose.
        assertEquals(0, levelToSave(changed = false, selected = 5, stored = 0))
    }

    @Test fun `a moved slider saves the new level`() {
        assertEquals(3, levelToSave(changed = true, selected = 3, stored = 7))
    }
}
