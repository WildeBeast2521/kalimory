package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.MaterialTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.DatabaseFileExport
import io.github.gonbei774.calisthenicsmemory.data.DatabaseStartupState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Shown instead of the app when the database cannot be used. Nothing here deletes data:
 * the user can export the raw files, restore a recovery file (which keeps the current files
 * aside), or, when the database itself opens, continue.
 */
@Composable
fun DatabaseUnavailableScreen(state: DatabaseStartupState, onContinue: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportResult by remember { mutableStateOf<String?>(null) }
    val exportDone = stringResource(R.string.db_export_done)
    val exportFailedFormat = stringResource(R.string.db_export_failed)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            exportResult = try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: throw IOException("Unable to open $uri for writing")
                    val entries = DatabaseFileExport.entries(context.getDatabasePath(AppDatabase.DATABASE_NAME))
                    output.use { DatabaseFileExport.writeZip(entries, it) }
                }
                exportDone
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                String.format(exportFailedFormat, e.message ?: e.toString())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.db_unavailable_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = when (state) {
                is DatabaseStartupState.Unsupported -> stringResource(R.string.db_unavailable_unsupported, state.installedVersion)
                is DatabaseStartupState.OpenFailed -> stringResource(R.string.db_unavailable_open_failed)
                DatabaseStartupState.CorruptionReported -> stringResource(R.string.db_unavailable_corruption_reported)
                DatabaseStartupState.Ready -> ""
            },
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.db_unavailable_export_hint),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Button(
            onClick = {
                val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                exportLauncher.launch("kalimory-database-$stamp.zip")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.db_export_files))
        }
        exportResult?.let { Text(text = it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) }
        // A recovery file exported earlier, or from another install, can replace these files.
        RecoveryRestoreButton()
        if (state == DatabaseStartupState.CorruptionReported) {
            OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.db_continue))
            }
        }
        if (state is DatabaseStartupState.OpenFailed) {
            Text(
                text = stringResource(R.string.db_error_details, state.details),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
