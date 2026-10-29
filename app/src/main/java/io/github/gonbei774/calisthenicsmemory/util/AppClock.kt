package io.github.gonbei774.calisthenicsmemory.util

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The clock every "today" and "now" on screen is read from. The app uses the system clock, read
 * afresh each time so a change of time zone applies at once, exactly like `LocalDate.now()`.
 * Screenshot tests set [fixed] so dates, weeks and charts render the same on every day.
 * `ConsistencyGuardTest` keeps `LocalDate.now()` and `LocalTime.now()` from being read elsewhere.
 */
object AppClock {
    /** A fixed clock for tests; null (always, in the app) means the system clock. */
    @Volatile
    var fixed: Clock? = null

    private fun clock(): Clock = fixed ?: Clock.systemDefaultZone()

    fun today(): LocalDate = LocalDate.now(clock())

    fun timeNow(): LocalTime = LocalTime.now(clock())

    fun now(): LocalDateTime = LocalDateTime.now(clock())
}
