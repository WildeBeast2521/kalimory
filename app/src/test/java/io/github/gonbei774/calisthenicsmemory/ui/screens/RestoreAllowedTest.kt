package io.github.gonbei774.calisthenicsmemory.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** A full restore overwrites everything, so it waits for a checked backup unless there is nothing to lose. */
class RestoreAllowedTest {
    @Test fun `data on the phone needs a checked backup first`() {
        assertEquals(false, restoreAllowed(hasUserData = true, backupVerified = false, busy = false))
        assertEquals(true, restoreAllowed(hasUserData = true, backupVerified = true, busy = false))
    }

    @Test fun `an empty database has nothing to back up`() {
        assertEquals(true, restoreAllowed(hasUserData = false, backupVerified = false, busy = false))
    }

    @Test fun `while the data is being counted, or anything runs, it waits`() {
        assertEquals(false, restoreAllowed(hasUserData = null, backupVerified = false, busy = false))
        assertEquals(false, restoreAllowed(hasUserData = true, backupVerified = true, busy = true))
        assertEquals(false, restoreAllowed(hasUserData = false, backupVerified = false, busy = true))
    }
}
