package io.github.gonbei774.calisthenicsmemory.data

import java.time.LocalDate

/**
 * Body weight and the load it adds to an exercise (ADR 0016). Pure: the same list gives the same
 * answer every time, so adding a past weight fixes past volume.
 */
object BodyWeightLoad {
    /**
     * The weight in grams that counts on [date]: the last entry on or before it, until the next one,
     * with no smoothing. Null before the first entry. [sorted] is ordered by date.
     */
    fun on(date: LocalDate, sorted: List<BodyWeight>): Int? {
        val key = date.toString()
        var low = 0
        var high = sorted.size - 1
        var found: BodyWeight? = null
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (sorted[mid].date <= key) { found = sorted[mid]; low = mid + 1 } else high = mid - 1
        }
        return found?.weightG
    }

    /** [on] for a stored date; null when the date cannot be read (a malformed one never crashes). */
    fun on(isoDate: String, sorted: List<BodyWeight>): Int? = runCatching { LocalDate.parse(isoDate) }.getOrNull()?.let { on(it, sorted) }

    /**
     * The load of one rep in grams: [percent] of [bodyG], plus added weight, minus assistance, never
     * below zero (assistance larger than the share means no load).
     */
    fun perRepG(percent: Int, bodyG: Int, addedG: Int?, assistanceG: Int?): Int =
        (percent.toLong() * bodyG / 100 + (addedG ?: 0) - (assistanceG ?: 0)).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

    /** The load as a share of body weight in whole percent, for the "heaviest load per body weight" figure. */
    fun percentOfBody(loadG: Int, bodyG: Int): Int = if (bodyG <= 0) 0 else (loadG * 100.0 / bodyG).toInt()
}
