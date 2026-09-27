package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One workout (ADR 0002). [sourceTemplateId] names the program or interval program it
 * came from, interpreted by [sourceType]; it is deliberately not a foreign key, so
 * deleting a template never deletes history. [sourceNameSnapshot] keeps what was run.
 */
@Entity(
    tableName = "workout_sessions",
    indices = [Index("startedAtEpochMillis"), Index("status")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: WorkoutSessionStatus,
    val sourceType: WorkoutSourceType,
    val sourceTemplateId: Long? = null,
    val sourceNameSnapshot: String? = null,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val updatedAtEpochMillis: Long,
    /** Precision of the start and end times; MINUTE for legacy-derived sessions. */
    val timePrecision: TimePrecision,
    val comment: String? = null,
    /**
     * The interval settings the workout ran with (INTERVAL_TEMPLATE sessions); null for other
     * sources. Added in database version 24.
     */
    val intervalWorkSeconds: Int? = null,
    val intervalRestSeconds: Int? = null,
    val intervalRounds: Int? = null,
    val intervalRoundRestSeconds: Int? = null,
)
