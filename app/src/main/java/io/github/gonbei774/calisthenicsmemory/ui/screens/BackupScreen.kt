package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.BackupPreferences
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupData
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDataPreview by remember { mutableStateOf(false) }
    var showBackupConfirmation by remember { mutableStateOf(false) }
    var showImportWarning by remember { mutableStateOf(false) }
    var pendingImportData by remember { mutableStateOf<BackupData?>(null) }
    var importFileName by remember { mutableStateOf<String?>(null) }
    var importGroupCount by remember { mutableStateOf(0) }
    var importExerciseCount by remember { mutableStateOf(0) }
    var importRecordCount by remember { mutableStateOf(0) }
    var importAnomalyCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    val backupPrefs = remember { BackupPreferences(context) }
    var lastBackupAt by remember { mutableStateOf(backupPrefs.getLastBackupAtMillis()) }
    val markBackedUp = {
        val now = System.currentTimeMillis()
        backupPrefs.setLastBackupAtMillis(now)
        lastBackupAt = now
    }

    // JSONエクスポート用ランチャー
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) scope.launch {
            isLoading = true
            try {
                when (val result = viewModel.exportData()) {
                    is BackupResult.Success -> {
                        withContext(Dispatchers.IO) {
                            BackupFileIo.writeUtf8(
                                { context.contentResolver.openOutputStream(uri) },
                                result.value.json
                            )
                        }
                        markBackedUp()
                        val summary = result.value.summary
                        viewModel.showSnackbar(UiMessage.ExportComplete(summary.groups, summary.exercises, summary.records))
                    }
                    is BackupResult.Failure -> viewModel.showSnackbar(UiMessage.ExportError(result.message))
                }
            } catch (_: CancellationException) {
                // Cancellation is not a failed export.
            } catch (e: IOException) {
                viewModel.showSnackbar(UiMessage.ExportError(e.message ?: ""))
            } finally {
                isLoading = false
            }
        }
    }

    // インポート前バックアップ用ランチャー（SAFでユーザーが保存先を選択）
    val backupBeforeImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                var success = false
                try {
                    when (val result = viewModel.exportData()) {
                        is BackupResult.Success -> {
                            withContext(Dispatchers.IO) {
                                BackupFileIo.writeUtf8(
                                    { context.contentResolver.openOutputStream(uri) },
                                    result.value.json
                                )
                            }
                            success = true
                            markBackedUp()
                            viewModel.showBackupResult(true)
                            showImportWarning = true
                        }
                        is BackupResult.Failure -> viewModel.showBackupResult(false)
                    }
                } catch (_: CancellationException) {
                    // Keep the parsed payload and do not report cancellation as failure.
                } catch (e: IOException) {
                    viewModel.showBackupResult(false)
                } finally {
                    isLoading = false
                    if (!success) showBackupConfirmation = true
                }
            }
        } else {
            showBackupConfirmation = true
        }
    }

    // JSONインポート用ランチャー. The selected URI is read exactly once.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) scope.launch {
            isLoading = true
            try {
                val fileName = withContext(Dispatchers.IO) {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
                    } ?: "unknown.json"
                }
                val jsonData = withContext(Dispatchers.IO) {
                    BackupFileIo.readUtf8(
                        openStream = { context.contentResolver.openInputStream(uri) }
                    )
                }
                if (viewModel.detectJsonFileType(jsonData) == "share") {
                    viewModel.showWrongFileTypeMessage(detected = "share", expected = "backup")
                } else when (val parsed = viewModel.parseBackupData(jsonData)) {
                    is BackupResult.Success -> {
                        val backupData = parsed.value.data
                        pendingImportData = backupData
                        importAnomalyCount = parsed.value.anomalies.size
                        importFileName = fileName
                        importGroupCount = backupData.groups.size
                        importExerciseCount = backupData.exercises.size
                        importRecordCount = backupData.records.size
                        showDataPreview = true
                    }
                    is BackupResult.Failure -> viewModel.showSnackbar(UiMessage.ImportError(parsed.message))
                }
            } catch (_: CancellationException) {
                // Cancellation is not a failed import.
            } catch (e: BackupSizeLimitException) {
                viewModel.showSnackbar(UiMessage.FileTooLarge(51, 50))
            } catch (e: IOException) {
                viewModel.showSnackbar(UiMessage.ImportError(e.message ?: ""))
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            AppIcons.Back,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(R.string.section_full_backup),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // セクション説明
            item {
                Text(
                    text = stringResource(R.string.section_full_backup_description),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // エクスポートボタン
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (!isLoading) {
                            val dateTime = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                            val fileName = "calisthenics_memory_backup_${dateTime.format(formatter)}.json"
                            exportLauncher.launch(fileName)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Upload, contentDescription = null, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.export_data),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = lastBackupAt?.let { stringResource(R.string.backup_last, formatBackupTime(it)) }
                                    ?: stringResource(R.string.backup_never),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // インポートボタン
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (!isLoading) {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.import_data),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.restore_from_backup),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 注意書き
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(AppIcons.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.warning_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.import_warning),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // ローディング表示
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // 第1段階: データ確認ダイアログ
    if (showDataPreview) {
        AlertDialog(
            onDismissRequest = {
                showDataPreview = false
                pendingImportData = null
            },
            title = {
                Text(
                    text = stringResource(R.string.import_data_preview_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ファイル名
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.file_name),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = importFileName ?: "unknown.json",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // データ件数
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.data_contents),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.groups),
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.count_items, importGroupCount),
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.exercises),
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.count_items, importExerciseCount),
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.records),
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.count_records, importRecordCount),
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (importAnomalyCount > 0) {
                        Text(
                            text = stringResource(R.string.import_anomalies_notice, importAnomalyCount),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            lineHeight = 20.sp
                        )
                    }

                    Text(
                        text = stringResource(R.string.import_data_preview_message),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDataPreview = false
                        showBackupConfirmation = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.next),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDataPreview = false
                        pendingImportData = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 第2段階: インポート警告ダイアログ
    if (showImportWarning) {
        AlertDialog(
            onDismissRequest = {
                showImportWarning = false
                pendingImportData = null
            },
            title = {
                Text(
                    text = stringResource(R.string.data_overwrite_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.existing_data_will_be_deleted),
                        fontSize = 16.sp
                    )
                    Text(
                        text = stringResource(R.string.cannot_undo),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportData?.let { backupData ->
                            scope.launch {
                                isLoading = true
                                try {
                                    when (val result = viewModel.importData(backupData)) {
                                        is BackupResult.Success -> {
                                            val summary = result.value
                                            viewModel.showSnackbar(UiMessage.ImportComplete(summary.groups, summary.exercises, summary.records))
                                            showImportWarning = false
                                            pendingImportData = null
                                        }
                                        is BackupResult.Failure -> {
                                            viewModel.showSnackbar(UiMessage.ImportError(result.message))
                                            // Keep both the dialog and exact parsed payload for retry.
                                        }
                                    }
                                } catch (_: CancellationException) {
                                    // Keep the payload; cancellation is not a failed restore.
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(R.string.import_confirm),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportWarning = false
                        pendingImportData = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // バックアップ確認ダイアログ
    if (showBackupConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showBackupConfirmation = false
            },
            title = {
                Text(
                    text = stringResource(R.string.backup_before_import_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.backup_before_import_message),
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )

                    Button(
                        onClick = {
                            showBackupConfirmation = false
                            val dateTime = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                            val fileName = "calisthenics_memory_backup_${dateTime.format(formatter)}.json"
                            backupBeforeImportLauncher.launch(fileName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.backup_and_continue),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            showBackupConfirmation = false
                            showImportWarning = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.skip_and_continue),
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackupConfirmation = false
                        pendingImportData = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/** The last backup time in the user's locale and time zone, for example "Sep 28, 2026, 9:57 PM". */
internal fun formatBackupTime(epochMillis: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String =
    java.time.Instant.ofEpochMilli(epochMillis).atZone(zone)
        .format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT))
