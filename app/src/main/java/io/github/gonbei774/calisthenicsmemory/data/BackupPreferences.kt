package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * When a full backup was last written, so the Backup screen can show it. Only a completed
 * export counts; a cancelled or failed one leaves the time unchanged.
 */
class BackupPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Wall-clock time of the last completed backup, or null if there has been none. */
    fun getLastBackupAtMillis(): Long? = prefs.getLong(KEY_LAST_BACKUP_AT, 0L).takeIf { it > 0L }

    fun setLastBackupAtMillis(millis: Long) {
        prefs.edit { putLong(KEY_LAST_BACKUP_AT, millis) }
    }

    private companion object {
        const val PREFS_NAME = "backup_prefs"
        const val KEY_LAST_BACKUP_AT = "last_backup_at_millis"
    }
}
