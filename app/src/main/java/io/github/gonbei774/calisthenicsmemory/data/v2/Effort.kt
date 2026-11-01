package io.github.gonbei774.calisthenicsmemory.data.v2

/**
 * A set's effort as the app records it (ADR 0012): how much was left at the end of the set, on a
 * five-step scale from 0 to [MAX_EFFORT_LEFT]. For a rep set it is reps in reserve (4 means "4 or
 * more"); for a hold it is the same scale in words, from "none, I was at my limit" to "plenty".
 * Null means the set was not rated. Nothing is ever inferred for a set the user did not rate.
 */
object Effort {
    const val MAX_EFFORT_LEFT = 4

    /** Ratings at or under this count as hard work for the body map's recovering view. */
    const val HARD_MAX_EFFORT_LEFT = 1

    fun isValid(effortLeft: Int?): Boolean = effortLeft == null || effortLeft in 0..MAX_EFFORT_LEFT

    /** A stored value as the app reads it: negative is not rated, above the scale is its top step. */
    fun normalized(effortLeft: Int?): Int? = effortLeft?.takeIf { it >= 0 }?.coerceAtMost(MAX_EFFORT_LEFT)

    fun isHard(effortLeft: Int?): Boolean = normalized(effortLeft)?.let { it <= HARD_MAX_EFFORT_LEFT } == true
}

/** Whether a set counts as work: every set except one the user marked as a warm-up. */
val HistorySet.countsAsWork: Boolean get() = !warmUp
