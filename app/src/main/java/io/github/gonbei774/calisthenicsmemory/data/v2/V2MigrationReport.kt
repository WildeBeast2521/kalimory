package io.github.gonbei774.calisthenicsmemory.data.v2

/** Why a legacy record was left on the legacy path instead of being converted. */
enum class LegacyAnomalyReason {
    /** date is not yyyy-MM-dd or time is not HH:mm. */
    UNPARSEABLE_DATE_TIME,
    /** The exercise type is neither "Dynamic" nor "Isometric". */
    UNKNOWN_EXERCISE_TYPE,
    /** The exercise laterality is neither "Bilateral" nor "Unilateral". */
    UNKNOWN_LATERALITY,
    /** A value, weight, distance, or assistance is negative. */
    NEGATIVE_VALUE,
    /**
     * A unilateral exercise's record has no left value: either the left side was not
     * done, or the exercise was bilateral when recorded. The side cannot be known.
     */
    AMBIGUOUS_SIDE,
    /** The record's exercise row does not exist. */
    MISSING_EXERCISE,
}

data class LegacyAnomaly(val trainingRecordId: Long, val reason: LegacyAnomalyReason)

/**
 * Outcome of one backfill run. Every legacy record is counted exactly once: as
 * converted now, already converted earlier, or left on the legacy path with a reason.
 */
data class V2MigrationReport(
    val legacyRecords: Int,
    val convertedRecords: Int,
    val alreadyConvertedRecords: Int,
    val sessionsCreated: Int,
    val setEntriesCreated: Int,
    val anomalies: List<LegacyAnomaly>,
) {
    val anomalyCounts: Map<LegacyAnomalyReason, Int> get() = anomalies.groupingBy { it.reason }.eachCount()
}
