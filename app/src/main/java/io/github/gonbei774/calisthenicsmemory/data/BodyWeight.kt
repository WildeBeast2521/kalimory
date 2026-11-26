package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * One body weight, from [date] until the next entry (ADR 0016). One entry per date, so the weight on
 * a day is never ambiguous. [importId] is the time of the import that added it (null for an entry
 * typed by hand or restored from a backup), so undoing an import can only remove what it added.
 */
@Entity(tableName = "body_weights", indices = [Index(value = ["date"], unique = true)])
data class BodyWeight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO date, "2026-11-11", in the form history dates use. */
    val date: String,
    val weightG: Int,
    val importId: Long? = null,
)

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weights ORDER BY date")
    fun observeAll(): Flow<List<BodyWeight>>

    @Query("SELECT * FROM body_weights WHERE date = :date")
    suspend fun onDate(date: String): BodyWeight?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(weight: BodyWeight): Long

    @Update
    suspend fun update(weight: BodyWeight)

    @Query("DELETE FROM body_weights WHERE id = :id")
    suspend fun delete(id: Long)

    /** Adds the entries of one import in a single transaction; a date that already has an entry is left as it is. */
    @Transaction
    suspend fun insertImported(rows: List<BodyWeight>) {
        rows.forEach { if (onDate(it.date) == null) insert(it) }
    }

    @Query("SELECT MAX(importId) FROM body_weights")
    suspend fun latestImportId(): Long?

    @Query("SELECT COUNT(*) FROM body_weights WHERE importId = :importId")
    suspend fun countImport(importId: Long): Int

    @Query("DELETE FROM body_weights WHERE importId = :importId")
    suspend fun deleteImport(importId: Long): Int
}
