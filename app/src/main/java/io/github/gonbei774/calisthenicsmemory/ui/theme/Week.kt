package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import java.time.DayOfWeek
import java.time.temporal.WeekFields

/** The user's chosen first day of the week, or null to follow the language and region. */
val LocalFirstDayOfWeekSetting = staticCompositionLocalOf<DayOfWeek?> { null }

/** The first day of the week every calendar-like view uses. */
@Composable
@ReadOnlyComposable
fun firstDayOfWeek(): DayOfWeek =
    LocalFirstDayOfWeekSetting.current ?: WeekFields.of(LocalConfiguration.current.locales[0]).firstDayOfWeek

/** The seven days of a week in display order, starting on [first]. */
fun weekDaysFrom(first: DayOfWeek): List<DayOfWeek> = (0L until 7L).map { first.plus(it) }
