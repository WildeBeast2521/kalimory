package io.github.gonbei774.calisthenicsmemory.ui.screens.catalogue

import io.github.gonbei774.calisthenicsmemory.data.progression.ChainClimb
import io.github.gonbei774.calisthenicsmemory.data.progression.ClimbMark
import java.time.LocalDate

/** A made-up climb: four steps over nine months, three of them met. */
internal object ClimbChartSamples {
    private fun d(s: String) = LocalDate.parse(s)
    val climb = ChainClimb(
        start = d("2026-01-05"),
        end = d("2026-10-08"),
        levels = listOf(d("2026-01-05") to 1, d("2026-02-04") to 2, d("2026-04-03") to 3, d("2026-06-01") to 4),
        marks = listOf(
            ClimbMark(d("2026-01-05"), 1, false), ClimbMark(d("2026-02-02"), 1, true),
            ClimbMark(d("2026-02-04"), 2, false), ClimbMark(d("2026-03-30"), 2, true),
            ClimbMark(d("2026-04-03"), 3, false), ClimbMark(d("2026-05-28"), 3, true),
            ClimbMark(d("2026-06-01"), 4, false),
        ),
    )
}
