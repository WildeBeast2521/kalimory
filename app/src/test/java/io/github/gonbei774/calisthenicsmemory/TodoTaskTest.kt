package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoTaskTest {
    private fun task(repeatDays: String) = TodoTask(id = 1, referenceId = 1, sortOrder = 0, repeatDays = repeatDays)

    @Test fun `parses well-formed schedules`() {
        assertEquals(emptyList<Int>(), TodoTask.parseRepeatDays(""))
        assertEquals(listOf(1, 3, 7), TodoTask.parseRepeatDays("1, 3,7"))
        assertEquals(listOf(1, 3, 7), task("1,3,7").getRepeatDayNumbers())
    }

    @Test fun `ignores malformed tokens instead of throwing`() {
        assertEquals(emptyList<Int>(), TodoTask.parseRepeatDays("x"))
        assertEquals(listOf(2), TodoTask.parseRepeatDays("0,2,8"))
        assertEquals(listOf(1, 2), TodoTask.parseRepeatDays("1,,2"))
        assertEquals(listOf(1), TodoTask.parseRepeatDays("1,1"))
        assertEquals(listOf(5), task("5,monday").getRepeatDayNumbers())
    }

    @Test fun `recognizes exactly the schedules the app writes`() {
        assertTrue(TodoTask.isValidRepeatDays(""))
        assertTrue(TodoTask.isValidRepeatDays("1, 3,7"))
        for (bad in listOf("x", "0", "8", "1,,2", "1,1", " ")) {
            assertFalse(bad, TodoTask.isValidRepeatDays(bad))
        }
    }

    @Test fun `reports whether the referenced item exists`() {
        val ids = setOf(1L)
        fun has(type: String, id: Long) = TodoTask(1, type, id, 0).hasTarget(ids, ids, ids, ids)
        for (type in listOf(TodoTask.TYPE_EXERCISE, TodoTask.TYPE_GROUP, TodoTask.TYPE_PROGRAM, TodoTask.TYPE_INTERVAL)) {
            assertEquals(type, true, has(type, 1))
            assertEquals(type, false, has(type, 2))
        }
        assertEquals(null, has("RETIRED_TYPE", 1))
    }

    @Test fun `due today follows the schedule and today's completion`() {
        val wednesday = java.time.LocalDate.of(2026, 9, 23)
        assertTrue(task("").isDueOn(wednesday))
        assertTrue(task("").copy(lastCompletedDate = "2026-09-23").isDueOn(wednesday))
        assertTrue(task("1,3").isDueOn(wednesday))
        assertFalse(task("1,3").copy(lastCompletedDate = "2026-09-23").isDueOn(wednesday))
        assertTrue(task("1,3").copy(lastCompletedDate = "2026-09-21").isDueOn(wednesday))
        assertFalse(task("1,2").isDueOn(wednesday))
        // A schedule with no valid day is never due, as before.
        assertFalse(task("x").isDueOn(wednesday))
    }
}
