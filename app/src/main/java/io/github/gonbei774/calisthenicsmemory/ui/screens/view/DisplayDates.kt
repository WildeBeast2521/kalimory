package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** A stored ISO date ("2026-09-25") in the user's locale ("Sep 25, 2026"); the raw text if it is not a date. */
fun formatStoredDate(isoDate: String, locale: Locale): String =
    runCatching { formatDate(LocalDate.parse(isoDate), locale) }.getOrDefault(isoDate)

fun formatDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

/** The date in the current locale, followed by the stored minute exactly as recorded. */
@Composable
fun displayDateTime(isoDate: String, time: String): String =
    "${formatStoredDate(isoDate, LocalConfiguration.current.locales[0])} $time"
