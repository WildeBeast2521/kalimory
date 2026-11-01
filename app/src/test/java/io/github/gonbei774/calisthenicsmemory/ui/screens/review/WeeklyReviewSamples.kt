package io.github.gonbei774.calisthenicsmemory.ui.screens.review

import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MovementPattern
import io.github.gonbei774.calisthenicsmemory.data.review.FamilySets
import io.github.gonbei774.calisthenicsmemory.data.review.MovementFamily
import io.github.gonbei774.calisthenicsmemory.data.review.WeekBest
import io.github.gonbei774.calisthenicsmemory.data.review.WeekReview
import io.github.gonbei774.calisthenicsmemory.data.review.WeekStep
import java.time.LocalDate

/** A made-up week for the review's screen tests: every kind of row, one group past the mark. */
internal object WeeklyReviewSamples {
    /** A real chain, so the step and chain names come from the catalogue. */
    val chain = Catalogue.chains.first { it.pattern == MovementPattern.HORIZONTAL_PULL }

    val week = WeekReview(
        weekStart = LocalDate.of(2026, 3, 9),
        daysTrained = 3,
        totalSets = 44,
        families = listOf(
            FamilySets(MovementFamily.PUSH, 12), FamilySets(MovementFamily.PULL, 7),
            FamilySets(MovementFamily.LEGS, 10), FamilySets(MovementFamily.CORE, 4),
        ),
        otherSets = 3,
        intervalSets = 8,
        bests = listOf(
            WeekBest(1, "Push-up", dynamic = true, value = 15, previousBest = 12, date = LocalDate.of(2026, 3, 11)),
            WeekBest(2, "Plank", dynamic = false, value = 75, previousBest = 60, date = LocalDate.of(2026, 3, 13)),
        ),
        stepsMet = listOf(WeekStep(chain.id, chain.steps[1], LocalDate.of(2026, 3, 12))),
    )
    val empty = WeekReview(LocalDate.of(2026, 3, 9), 0, 0, emptyList(), 0, 0)
    const val RANGE = "9 Mar 2026 – 15 Mar 2026"
}
