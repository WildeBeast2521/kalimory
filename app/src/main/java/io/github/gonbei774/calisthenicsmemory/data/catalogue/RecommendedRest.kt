package io.github.gonbei774.calisthenicsmemory.data.catalogue

import io.github.gonbei774.calisthenicsmemory.data.Exercise

/**
 * A sensible rest between sets for this step, in seconds. Mobility needs little rest and
 * conditioning stays brisk; strength and skill work rest longer as the step gets harder, from a
 * minute for the easiest steps to three minutes for the hardest skills.
 */
fun CatalogueStep.recommendedRestSeconds(): Int = when (Catalogue.chain(chainId)?.pattern) {
    MovementPattern.MOBILITY -> 30
    MovementPattern.CONDITIONING -> 60
    else -> when (difficulty) {
        in 1..2 -> 60
        in 3..4 -> 90
        in 5..6 -> 120
        in 7..8 -> 150
        else -> 180
    }
}

/** The rest an exercise's catalogue step suggests, or null for an exercise not from the catalogue. */
fun Exercise.recommendedRestSeconds(): Int? = catalogId?.let { Catalogue.step(it) }?.recommendedRestSeconds()
