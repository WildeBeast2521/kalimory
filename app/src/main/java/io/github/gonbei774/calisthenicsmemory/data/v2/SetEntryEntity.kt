package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One set attempt. Metrics are typed columns with units in their names; null means
 * not recorded, and a skip is [status], never a magic value. [legacyTrainingRecordId]
 * marks entries converted from a legacy record and keeps the conversion idempotent.
 * Values must not be negative; Room cannot declare CHECK constraints, so the write
 * path enforces this.
 */
@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionExerciseId", "orderIndex"], unique = true),
        Index(value = ["legacyTrainingRecordId"], unique = true),
    ],
)
data class SetEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val orderIndex: Int,
    val setNumber: Int,
    val roundNumber: Int? = null,
    val status: SetEntryStatus,
    val side: BodySide,
    val repetitions: Int? = null,
    val durationMillis: Long? = null,
    val distanceCm: Int? = null,
    val addedWeightGrams: Int? = null,
    val assistanceGrams: Int? = null,
    val targetRepetitions: Int? = null,
    val targetDurationMillis: Long? = null,
    val startedAtEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null,
    val timePrecision: TimePrecision,
    val comment: String? = null,
    val legacyTrainingRecordId: Long? = null,
)
