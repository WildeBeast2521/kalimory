package io.github.gonbei774.calisthenicsmemory.viewmodel

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.v2.CompatibilityHistory
import java.time.LocalDate
import java.time.ZoneId

/**
 * The To Do list: adding entries at its end, and completing one. A repeating entry is marked done
 * for the day; any other is removed. Finishing an exercise also completes its group's entry once
 * every exercise in the group has a set that day.
 */
class TodoActions(private val database: AppDatabase) {
    private val todoTaskDao = database.todoTaskDao()

    /** Adds one entry of [type] for each of [referenceIds], in that order, after the existing ones. */
    suspend fun add(type: String, referenceIds: List<Long>) {
        database.withTransaction {
            var sortOrder = todoTaskDao.getNextSortOrder()
            referenceIds.forEach { id ->
                todoTaskDao.insert(TodoTask(type = type, referenceId = id, sortOrder = sortOrder++))
            }
        }
    }

    /** Completes the entry of [type] and [referenceId] on [today]; history days are read in [zone]. */
    suspend fun complete(type: String, referenceId: Long, today: LocalDate, zone: ZoneId) {
        val task = todoTaskDao.getTaskByReference(type, referenceId)
        if (task != null && task.isRepeating()) {
            todoTaskDao.updateLastCompletedDate(type, referenceId, today.toString())
        } else {
            todoTaskDao.deleteByReference(type, referenceId)
        }
        if (type == TodoTask.TYPE_EXERCISE) completeGroupIfDone(referenceId, today, zone)
    }

    private suspend fun completeGroupIfDone(exerciseId: Long, today: LocalDate, zone: ZoneId) {
        val groupName = database.exerciseDao().getExerciseById(exerciseId)?.group ?: return
        val group = database.exerciseGroupDao().getGroupByName(groupName) ?: return
        val groupTask = todoTaskDao.getTaskByReference(TodoTask.TYPE_GROUP, group.id) ?: return

        // A repeating group entry counts only on its days, and once a day.
        if (groupTask.isRepeating()) {
            if (today.dayOfWeek.value !in groupTask.getRepeatDayNumbers()) return
            if (groupTask.lastCompletedDate == today.toString()) return
        }

        val allDone = database.exerciseDao().getExercisesByGroup(groupName).all {
            CompatibilityHistory.hasSetOn(database, it.id, today, zone)
        }
        if (!allDone) return
        if (groupTask.isRepeating()) {
            todoTaskDao.updateLastCompletedDate(TodoTask.TYPE_GROUP, group.id, today.toString())
        } else {
            todoTaskDao.deleteByReference(TodoTask.TYPE_GROUP, group.id)
        }
    }
}
