package io.github.gonbei774.calisthenicsmemory.data.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** The persisted codes are part of the database format and must never change. */
class WorkoutEnumsTest {
    private inline fun <reified E> codes(): List<String> where E : Enum<E>, E : StableCode =
        enumValues<E>().map { it.code }

    @Test fun `persisted codes are exactly these`() {
        assertEquals(listOf("PLANNED", "ACTIVE", "PAUSED", "COMPLETED", "ABANDONED"), codes<WorkoutSessionStatus>())
        assertEquals(listOf("AD_HOC", "PROGRAM_TEMPLATE", "INTERVAL_TEMPLATE", "LEGACY_IMPORT", "MANUAL"), codes<WorkoutSourceType>())
        assertEquals(listOf("PENDING", "COMPLETED", "SKIPPED"), codes<SetEntryStatus>())
        assertEquals(listOf("BILATERAL", "RIGHT", "LEFT"), codes<BodySide>())
        assertEquals(listOf("DYNAMIC", "ISOMETRIC"), codes<ExerciseKind>())
        assertEquals(listOf("BILATERAL", "UNILATERAL"), codes<Laterality>())
        assertEquals(listOf("EXACT", "MINUTE"), codes<TimePrecision>())
    }

    @Test fun `converters round trip every value and reject unknown codes`() {
        val c = WorkoutTypeConverters()
        WorkoutSessionStatus.entries.forEach { assertEquals(it, c.codeToSessionStatus(c.sessionStatusToCode(it))) }
        WorkoutSourceType.entries.forEach { assertEquals(it, c.codeToSourceType(c.sourceTypeToCode(it))) }
        SetEntryStatus.entries.forEach { assertEquals(it, c.codeToSetStatus(c.setStatusToCode(it))) }
        BodySide.entries.forEach { assertEquals(it, c.codeToBodySide(c.bodySideToCode(it))) }
        ExerciseKind.entries.forEach { assertEquals(it, c.codeToExerciseKind(c.exerciseKindToCode(it))) }
        Laterality.entries.forEach { assertEquals(it, c.codeToLaterality(c.lateralityToCode(it))) }
        TimePrecision.entries.forEach { assertEquals(it, c.codeToTimePrecision(c.timePrecisionToCode(it))) }

        assertThrows(IllegalArgumentException::class.java) { c.codeToSetStatus("DONE") }
        assertThrows(IllegalArgumentException::class.java) { c.codeToBodySide("bilateral") }
    }
}
