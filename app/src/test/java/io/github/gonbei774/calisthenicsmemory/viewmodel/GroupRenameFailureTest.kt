package io.github.gonbei774.calisthenicsmemory.viewmodel

import android.database.sqlite.SQLiteConstraintException
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Renaming a group to a name that is taken says so, instead of a generic error. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GroupRenameFailureTest {
    @Test fun `a taken name is reported as already in use`() {
        assertSame(UiMessage.GroupAlreadyExists, groupRenameFailure(SQLiteConstraintException("UNIQUE constraint failed")))
    }

    @Test fun `anything else is a general error`() {
        assertSame(UiMessage.ErrorOccurred, groupRenameFailure(IllegalStateException("closed")))
    }
}
