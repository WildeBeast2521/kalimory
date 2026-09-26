package io.github.gonbei774.calisthenicsmemory.data.v2

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup

/**
 * One ordered exercise occurrence in a workout. Links to the current library
 * ([exerciseId], [groupId]) become null when those rows are deleted; the snapshots
 * keep the history readable. [sourceProgramExerciseId] is a plain reference.
 */
@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ExerciseGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["workoutSessionId", "orderIndex"], unique = true),
        Index("exerciseId"),
        Index("groupId"),
    ],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutSessionId: Long,
    val orderIndex: Int,
    val exerciseId: Long?,
    val groupId: Long? = null,
    val sourceProgramExerciseId: Long? = null,
    val exerciseNameSnapshot: String,
    val exerciseKindSnapshot: ExerciseKind,
    val lateralitySnapshot: Laterality,
    val groupNameSnapshot: String? = null,
    val targetSets: Int? = null,
    val targetRepetitions: Int? = null,
    val targetDurationMillis: Long? = null,
)
