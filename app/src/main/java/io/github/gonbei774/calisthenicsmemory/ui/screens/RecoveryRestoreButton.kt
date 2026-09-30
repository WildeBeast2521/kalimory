package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.RecoveryRestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

private sealed interface RestoreOutcome {
    /** The database was replaced, or a failed replace was rolled back; either way the app restarts. */
    data class Restart(val message: String?) : RestoreOutcome
    /** The file was refused before anything changed. */
    data class Refused(val message: String) : RestoreOutcome
}

/**
 * Restores the raw database from a recovery file, after the user confirms. The current files
 * are kept (see [RecoveryRestore]), and the app restarts so every screen reads the new data.
 */
@Composable
fun RecoveryRestoreButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<RestoreOutcome?>(null) }
    val messages = RestoreMessages(
        noDatabase = stringResource(R.string.recovery_rejected_no_database),
        tooLarge = stringResource(R.string.recovery_rejected_too_large),
        version = stringResource(R.string.recovery_rejected_version),
        damaged = stringResource(R.string.recovery_rejected_damaged),
        failed = stringResource(R.string.recovery_restore_failed),
        unreadable = stringResource(R.string.recovery_rejected_unreadable),
    )

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { outcome = restore(context, uri, messages) }
    }

    OutlinedButton(onClick = { confirming = true }, modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.recovery_restore_button))
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.recovery_restore_title)) },
            text = { Text(stringResource(R.string.recovery_restore_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    picker.launch(arrayOf("application/zip", "application/octet-stream"))
                }) { Text(stringResource(R.string.recovery_restore_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    when (val result = outcome) {
        is RestoreOutcome.Refused -> AlertDialog(
            onDismissRequest = { outcome = null },
            text = { Text(result.message) },
            confirmButton = { TextButton(onClick = { outcome = null }) { Text(stringResource(R.string.ok)) } },
        )
        is RestoreOutcome.Restart -> AlertDialog(
            onDismissRequest = {},
            text = { Text(result.message ?: stringResource(R.string.recovery_restore_done)) },
            confirmButton = { TextButton(onClick = { restartApp(context) }) { Text(stringResource(R.string.ok)) } },
        )
        null -> Unit
    }
}

private class RestoreMessages(
    val noDatabase: String,
    val tooLarge: String,
    val version: String,
    val damaged: String,
    val failed: String,
    val unreadable: String,
)

private suspend fun restore(context: Context, uri: Uri, messages: RestoreMessages): RestoreOutcome = withContext(Dispatchers.IO) {
    val database = context.getDatabasePath(AppDatabase.DATABASE_NAME)
    val staging = File(context.cacheDir, "recovery-restore")
    try {
        val check = try {
            context.contentResolver.openInputStream(uri)?.use {
                RecoveryRestore.stageAndCheck(it, AppDatabase.DATABASE_NAME, staging, AppDatabase.OLDEST_SUPPORTED_VERSION..AppDatabase.CURRENT_VERSION)
            } ?: throw IOException(uri.toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext RestoreOutcome.Refused(String.format(messages.unreadable, e.message ?: e.toString()))
        }
        when (check) {
            is RecoveryRestore.Check.Rejected -> RestoreOutcome.Refused(
                when (check.problem) {
                    RecoveryRestore.Problem.NO_DATABASE -> messages.noDatabase
                    RecoveryRestore.Problem.TOO_LARGE -> messages.tooLarge
                    RecoveryRestore.Problem.UNSUPPORTED_VERSION -> String.format(messages.version, check.version ?: 0)
                    RecoveryRestore.Problem.DAMAGED -> messages.damaged
                }
            )
            is RecoveryRestore.Check.Ready -> {
                // From here the open database is closed, so the app restarts whatever happens.
                AppDatabase.closeForRestore()
                val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                try {
                    RecoveryRestore.install(check.staged, database, stamp)
                    RestoreOutcome.Restart(null)
                } catch (e: IOException) {
                    RestoreOutcome.Restart(String.format(messages.failed, e.message ?: e.toString()))
                }
            }
        }
    } finally {
        staging.deleteRecursively()
    }
}

/** Starts the app afresh, so the database and every screen open again from the restored files. */
private fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    if (intent != null) context.startActivity(intent)
    exitProcess(0)
}
