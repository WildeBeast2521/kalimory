package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.CrashReports
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * After a crash, offers the kept report once (CrashReports). "Share report" opens the share sheet
 * and "Delete" discards it; either way the report is deleted. Closing the dialog any other way
 * keeps it for the next launch.
 */
@Composable
fun CrashReportOffer() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reports = remember { CrashReports.forApp(context) }
    var pending by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        pending = withContext(Dispatchers.IO) { reports.pending() }
    }
    if (pending.isEmpty()) return

    val shareTitle = stringResource(R.string.crash_report_share)
    fun finish() {
        pending = emptyList()
        scope.launch(Dispatchers.IO) { reports.clear() }
    }
    AlertDialog(
        onDismissRequest = { pending = emptyList() },
        title = { Text(stringResource(R.string.crash_report_title)) },
        text = { Text(stringResource(R.string.crash_report_message)) },
        confirmButton = {
            TextButton(onClick = {
                shareCrashReports(context, CrashReports.shareText(pending), shareTitle)
                finish()
            }) { Text(shareTitle) }
        },
        dismissButton = { TextButton(onClick = { finish() }) { Text(stringResource(R.string.delete)) } },
    )
}

/** The share sheet with the report as plain text; the user picks where it goes, if anywhere. */
private fun shareCrashReports(context: Context, text: String, title: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Kalimory crash report")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, title))
}
