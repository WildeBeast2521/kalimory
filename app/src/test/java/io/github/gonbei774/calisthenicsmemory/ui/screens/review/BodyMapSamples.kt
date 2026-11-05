package io.github.gonbei774.calisthenicsmemory.ui.screens.review

import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.data.review.MuscleSets
import io.github.gonbei774.calisthenicsmemory.data.review.MuscleVolumeSummary
import java.time.LocalDate

/** A made-up week for the body map's screen tests, with every shade and every row not on the map. */
internal object BodyMapSamples {
    val week = MuscleVolumeSummary(
        start = LocalDate.of(2026, 3, 9),
        end = LocalDate.of(2026, 3, 15),
        muscles = listOf(
            MuscleSets(Muscle.CHEST, 12, 0),
            MuscleSets(Muscle.TRICEPS, 3, 8),
            MuscleSets(Muscle.LATS, 6, 0),
            MuscleSets(Muscle.ABDOMINALS, 0, 6),
            MuscleSets(Muscle.QUADRICEPS, 2, 0),
        ),
        mappedSets = 23,
        mobilitySets = 4,
        otherSets = 3,
        intervalSets = 8,
    )
    val empty = MuscleVolumeSummary(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 15), emptyList(), 0, 0, 0, 0)
}
