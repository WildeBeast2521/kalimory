package io.github.gonbei774.calisthenicsmemory.data.progression

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Query
import androidx.room.Upsert
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * Places one of the user's own exercises in a built-in chain (ADR 0007), after [afterStepId], or
 * first when that is null. Chain and step ids are catalogue ids (ADR 0006). An exercise sits in
 * at most one built-in chain; deleting the exercise removes its placement.
 */
@Entity(
    tableName = "chain_placements",
    primaryKeys = ["exerciseId"],
    foreignKeys = [
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["chainId"])],
)
data class ChainPlacement(
    val exerciseId: Long,
    val chainId: String,
    val afterStepId: String? = null,
)

@Dao
interface ChainPlacementDao {
    @Query("SELECT * FROM chain_placements ORDER BY exerciseId")
    fun observeAll(): Flow<List<ChainPlacement>>

    @Upsert
    suspend fun upsert(placement: ChainPlacement)

    @Query("DELETE FROM chain_placements WHERE exerciseId = :exerciseId")
    suspend fun remove(exerciseId: Long)
}
