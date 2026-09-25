package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String = TYPE_EXERCISE,
    val referenceId: Long,
    val sortOrder: Int,
    val repeatDays: String = "",
    val lastCompletedDate: String? = null
) {
    companion object {
        const val TYPE_EXERCISE = "EXERCISE"
        const val TYPE_GROUP = "GROUP"
        const val TYPE_PROGRAM = "PROGRAM"
        const val TYPE_INTERVAL = "INTERVAL"

        // Day numbers 1 (Monday) to 7 (Sunday). Malformed tokens, which older imports could
        // store, are skipped rather than thrown on; the stored value is left unchanged.
        fun parseRepeatDays(value: String): List<Int> =
            if (value.isEmpty()) emptyList()
            else value.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.distinct()

        // True for the format the app writes: empty, or distinct day numbers 1..7 separated by commas.
        fun isValidRepeatDays(value: String): Boolean {
            if (value.isEmpty()) return true
            val days = value.split(",").map { it.trim().toIntOrNull() }
            return days.all { it != null && it in 1..7 } && days.distinct().size == days.size
        }
    }

    fun isRepeating(): Boolean = repeatDays.isNotEmpty()

    fun getRepeatDayNumbers(): List<Int> = parseRepeatDays(repeatDays)
}
