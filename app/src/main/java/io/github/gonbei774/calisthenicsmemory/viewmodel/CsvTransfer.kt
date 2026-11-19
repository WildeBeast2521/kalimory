package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.data.v2.CsvWorkoutImport
import io.github.gonbei774.calisthenicsmemory.data.v2.CompatibilityHistory
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.ZoneId

/**
 * CSV export and merge import of groups, exercises and records. Exercises and groups are read
 * from the database as they stand, and [onMessage] receives what the screen should say.
 */
class CsvTransfer(
    private val database: AppDatabase,
    private val onMessage: (UiMessage) -> Unit,
) {
    private val exerciseDao = database.exerciseDao()
    private val groupDao = database.exerciseGroupDao()
    private val recordDao = database.trainingRecordDao()

    // ========================================
    // CSV エクスポート・インポート機能
    // ========================================

    /**
     * グループCSVをエクスポート
     */
    suspend fun exportGroups(): String = withContext(Dispatchers.IO) {
        try {
            val currentGroups = groupDao.getAllGroupsSync()

            val csvBuilder = StringBuilder()
            csvBuilder.appendLine("name")

            currentGroups.forEach { group ->
                csvBuilder.appendLine(csvEscape(group.name))
            }

            onMessage(UiMessage.CsvExportSuccess("Groups", currentGroups.size))

            csvBuilder.toString()
        } catch (e: Exception) {
            onMessage(UiMessage.ExportError(e.message ?: ""))
            throw e
        }
    }

    /**
     * 種目CSVをエクスポート
     */
    suspend fun exportExercises(): String = withContext(Dispatchers.IO) {
        try {
            val currentExercises = exerciseDao.getAllExercises().first()

            val csvBuilder = StringBuilder()
            csvBuilder.appendLine("name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId")

            currentExercises.forEach { exercise ->
                csvBuilder.appendLine(
                    csvRow(
                        exercise.name,
                        exercise.type,
                        exercise.group ?: "",
                        exercise.sortOrder.toString(),
                        exercise.laterality,
                        exercise.targetSets?.toString() ?: "",
                        exercise.targetValue?.toString() ?: "",
                        exercise.isFavorite.toString(),
                        exercise.displayOrder.toString(),
                        exercise.restInterval?.toString() ?: "",
                        exercise.repDuration?.toString() ?: "",
                        exercise.distanceTrackingEnabled.toString(),
                        exercise.weightTrackingEnabled.toString(),
                        exercise.assistanceTrackingEnabled.toString(),
                        exercise.description ?: "",
                        exercise.catalogId ?: ""
                    )
                )
            }

            onMessage(UiMessage.CsvExportSuccess("Exercises", currentExercises.size))

            csvBuilder.toString()
        } catch (e: Exception) {
            onMessage(UiMessage.ExportError(e.message ?: ""))
            throw e
        }
    }

    /**
     * 記録入力用テンプレートCSVをエクスポート
     * 種目リスト + コメント例
     */
    suspend fun exportRecordTemplate(): String = withContext(Dispatchers.IO) {
        try {
            val currentExercises = exerciseDao.getAllExercises().first()

            val csvBuilder = StringBuilder()

            // Header: 13 columns, the last two (warmUp, effortLeft) optional on import
            csvBuilder.appendLine(RECORDS_CSV_HEADER)

            // 入力例（コメント - 英語のみ）
            csvBuilder.appendLine("# Example: Multiple sets with same date/time (one session)")
            csvBuilder.appendLine("# Wall Push-up,Dynamic,2025-11-09,10:00,1,20,,Morning session,,,")
            csvBuilder.appendLine("# Wall Push-up,Dynamic,2025-11-09,10:00,2,19,,Morning session,,,")
            csvBuilder.appendLine("# Wall Push-up,Dynamic,2025-11-09,10:00,3,15,,Morning session,,,")
            csvBuilder.appendLine("# Unilateral exercise example (with valueLeft)")
            csvBuilder.appendLine("# One-leg Squat,Dynamic,2025-11-09,10:30,1,8,7,Right leg stronger,,,")
            csvBuilder.appendLine("# With distance (cm) and weight (g)")
            csvBuilder.appendLine("# Running,Dynamic,2025-11-09,08:00,1,1,,5km run,500000,,")
            csvBuilder.appendLine("# Weighted Push-up,Dynamic,2025-11-09,10:00,1,10,,With vest,,10000,")
            csvBuilder.appendLine("# With assistance (g) - band assisted")
            csvBuilder.appendLine("# Pull-up,Dynamic,2025-11-09,11:00,1,5,,With band,,,22000")
            csvBuilder.appendLine("# Warm-up mark (warmUp true) and effort (effortLeft 0-4, reps in reserve)")
            csvBuilder.appendLine("# Wall Push-up,Dynamic,2025-11-09,09:55,1,12,,Warm-up,,,,true,4")
            csvBuilder.appendLine("#")

            // 種目リスト（空欄テンプレート）
            currentExercises.forEach { exercise ->
                csvBuilder.appendLine(csvRow(exercise.name, exercise.type) + ",,,,,,,,,,,")
            }

            onMessage(UiMessage.CsvTemplateExported(currentExercises.size))

            csvBuilder.toString()
        } catch (e: Exception) {
            onMessage(UiMessage.ExportError(e.message ?: ""))
            throw e
        }
    }

    /**
     * 実際の記録データをCSV形式でエクスポート (v12: 11列)
     */
    suspend fun exportRecords(): String = withContext(Dispatchers.IO) {
        try {
            // Every set in history, legacy or v2, so v2-only workouts are exported too.
            val currentRecords = CompatibilityHistory.read(database, ZoneId.systemDefault())
            val csv = recordsCsv(currentRecords, exerciseDao.getAllExercises().first())

            onMessage(UiMessage.CsvExportSuccess("Records", currentRecords.size))

            csv
        } catch (e: Exception) {
            onMessage(UiMessage.ExportError(e.message ?: ""))
            throw e
        }
    }

    /**
     * グループCSVをインポート（マージモード）
     */
    suspend fun importGroups(csvString: String): CsvImportReport = withContext(Dispatchers.IO) {
        var successCount = 0
        var skippedCount = 0
        var errorCount = 0
        val skippedItems = mutableListOf<String>()
        val errors = mutableListOf<String>()

        try {
            val records = parseCsvRecords(csvString)

            if (records.isEmpty()) {
                onMessage(UiMessage.CsvEmpty)
                return@withContext CsvImportReport(CsvType.GROUPS, 0, 0, 0, emptyList(), emptyList())
            }

            // ヘッダー行をスキップ
            val dataRecords = records.drop(1)

            dataRecords.forEachIndexed { index, record ->
                try {
                    val name = record.firstOrNull()?.trim() ?: ""

                    if (name.isEmpty()) {
                        errors.add("Line ${index + 2}: Group name is empty")
                        errorCount++
                        return@forEachIndexed
                    }

                    // 重複チェック
                    val existing = groupDao.getGroupByName(name)
                    if (existing != null) {
                        skippedItems.add("\"$name\" (already exists)")
                        skippedCount++
                        return@forEachIndexed
                    }

                    // After the existing groups, as a group added by hand is.
                    groupDao.insertGroup(ExerciseGroup(name = name, displayOrder = groupDao.nextGroupDisplayOrder()))
                    successCount++

                } catch (e: Exception) {
                    errors.add("Line ${index + 2}: ${e.message}")
                    errorCount++
                }
            }

        } catch (e: Exception) {
            onMessage(UiMessage.ImportError(e.message ?: ""))
        }

        return@withContext CsvImportReport(CsvType.GROUPS, successCount, skippedCount, errorCount, skippedItems, errors)
    }

    /**
     * 種目CSVをインポート（マージモード）
     * - 8列（旧フォーマット）と11列（新フォーマット）の両方に対応
     * - グループが存在しない場合は自動作成
     */
    suspend fun importExercises(csvString: String): CsvImportReport = withContext(Dispatchers.IO) {
        var successCount = 0
        var skippedCount = 0
        var errorCount = 0
        var groupsCreatedCount = 0
        val skippedItems = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val catalogIdsSeen = mutableSetOf<String>()

        try {
            val records = parseCsvRecords(csvString)

            if (records.isEmpty()) {
                onMessage(UiMessage.CsvEmpty)
                return@withContext CsvImportReport(CsvType.EXERCISES, 0, 0, 0, emptyList(), emptyList())
            }

            // ヘッダー行をスキップ
            val dataRecords = records.drop(1)

            dataRecords.forEachIndexed { index, columns ->
                try {
                    // 8列（旧フォーマット）または11列（新フォーマット）を許容
                    if (columns.size < 8) {
                        errors.add("Line ${index + 2}: Invalid format (expected at least 8 columns, got ${columns.size})")
                        errorCount++
                        return@forEachIndexed
                    }

                    val name = columns[0].trim()
                    val type = columns[1].trim()
                    val group = columns[2].trim().ifEmpty { null }
                    val sortOrderStr = columns[3].trim()
                    val laterality = columns[4].trim()
                    val targetSetsStr = columns[5].trim()
                    val targetValueStr = columns[6].trim()
                    val isFavoriteStr = columns[7].trim()

                    // 新フィールド（9列目以降、オプション）
                    val displayOrderStr = columns.getOrNull(8)?.trim() ?: ""
                    val restIntervalStr = columns.getOrNull(9)?.trim() ?: ""
                    val repDurationStr = columns.getOrNull(10)?.trim() ?: ""
                    val distanceTrackingEnabledStr = columns.getOrNull(11)?.trim() ?: ""
                    val weightTrackingEnabledStr = columns.getOrNull(12)?.trim() ?: ""
                    val assistanceTrackingEnabledStr = columns.getOrNull(13)?.trim() ?: ""
                    val descriptionStr = columns.getOrNull(14)?.trim() ?: ""
                    val catalogIdStr = columns.getOrNull(15)?.trim() ?: ""

                    // 必須フィールドチェック
                    if (name.isEmpty() || type.isEmpty() || laterality.isEmpty()) {
                        errors.add("Line ${index + 2}: Missing required fields (name, type, or laterality)")
                        errorCount++
                        return@forEachIndexed
                    }

                    // バリデーション
                    if (type !in listOf("Dynamic", "Isometric")) {
                        errors.add("Line ${index + 2}: Invalid type \"$type\" (must be Dynamic or Isometric)")
                        errorCount++
                        return@forEachIndexed
                    }

                    if (laterality !in listOf("Bilateral", "Unilateral")) {
                        errors.add("Line ${index + 2}: Invalid laterality \"$laterality\" (must be Bilateral or Unilateral)")
                        errorCount++
                        return@forEachIndexed
                    }

                    // The catalogue link, when the file has one: a malformed id is refused before anything is created.
                    if (catalogIdStr.isNotEmpty() && (catalogIdStr.length > MAX_CATALOG_ID_LENGTH || !CATALOG_ID_PATTERN.matches(catalogIdStr))) {
                        errors.add("Line ${index + 2}: Invalid catalogId \"$catalogIdStr\"")
                        errorCount++
                        return@forEachIndexed
                    }

                    // グループの存在チェック（なければ自動作成）
                    if (group != null) {
                        val groupExists = groupDao.getGroupByName(group) != null
                        if (!groupExists) {
                            // グループを自動作成
                            val newGroup = ExerciseGroup(name = group, displayOrder = groupDao.nextGroupDisplayOrder())
                            groupDao.insertGroup(newGroup)
                            groupsCreatedCount++
                        }
                    }

                    // 数値変換
                    val sortOrder = sortOrderStr.toIntOrNull() ?: 0
                    val targetSets = targetSetsStr.toIntOrNull()
                    val targetValue = targetValueStr.toIntOrNull()
                    val isFavorite = isFavoriteStr.toBooleanStrictOrNull() ?: false
                    val displayOrder = displayOrderStr.toIntOrNull() ?: 0
                    val restInterval = restIntervalStr.toIntOrNull()
                    val repDuration = repDurationStr.toIntOrNull()
                    val distanceTrackingEnabled = distanceTrackingEnabledStr.toBooleanStrictOrNull() ?: false
                    val weightTrackingEnabled = weightTrackingEnabledStr.toBooleanStrictOrNull() ?: false
                    val assistanceTrackingEnabled = assistanceTrackingEnabledStr.toBooleanStrictOrNull() ?: false
                    val description = descriptionStr.ifEmpty { null }

                    // 重複チェック
                    val existing = exerciseDao.getExerciseByNameAndType(name, type)
                    if (existing != null) {
                        // laterality不一致の場合はスキップ
                        if (existing.laterality != laterality) {
                            skippedItems.add("\"$name, $type\" (laterality mismatch: existing=${existing.laterality}, CSV=$laterality)")
                            skippedCount++
                            return@forEachIndexed
                        }
                        // 完全一致の場合もスキップ。The existing exercise's catalogue link is never changed.
                        val linkNote = if (catalogIdStr.isNotEmpty() && catalogIdStr != existing.catalogId) "; catalogue link not changed" else ""
                        skippedItems.add("\"$name, $type\" (already exists$linkNote)")
                        skippedCount++
                        return@forEachIndexed
                    }

                    // A catalogue step can be linked to one exercise only: one that is already linked here, or
                    // repeated in this file, comes in without its link and says so.
                    val catalogId = catalogIdStr.ifEmpty { null }?.takeIf { id ->
                        val taken = exerciseDao.getExerciseByCatalogId(id) != null || !catalogIdsSeen.add(id)
                        if (taken) skippedItems.add("\"$name, $type\" (added without its catalogue link: that step is already linked)")
                        !taken
                    }

                    val exercise = Exercise(
                        name = name,
                        type = type,
                        group = group,
                        catalogId = catalogId,
                        sortOrder = sortOrder,
                        displayOrder = displayOrder,
                        laterality = laterality,
                        targetSets = targetSets,
                        targetValue = targetValue,
                        isFavorite = isFavorite,
                        restInterval = restInterval,
                        repDuration = repDuration,
                        distanceTrackingEnabled = distanceTrackingEnabled,
                        weightTrackingEnabled = weightTrackingEnabled,
                        assistanceTrackingEnabled = assistanceTrackingEnabled,
                        description = description
                    )
                    exerciseDao.insertExercise(exercise)
                    successCount++

                } catch (e: Exception) {
                    errors.add("Line ${index + 2}: ${e.message}")
                    errorCount++
                }
            }

        } catch (e: Exception) {
            onMessage(UiMessage.ImportError(e.message ?: ""))
        }

        return@withContext CsvImportReport(
            type = CsvType.EXERCISES,
            successCount = successCount,
            skippedCount = skippedCount,
            errorCount = errorCount,
            skippedItems = skippedItems,
            errors = errors,
            groupsCreatedCount = groupsCreatedCount
        )
    }

    /**
     * Imports records from a CSV file, adding to what is there and skipping what is already there. Rows become
     * workouts in the current history model (see [CsvWorkoutImport]), all in one transaction: if writing fails,
     * nothing is added.
     */
    suspend fun importRecordsFromCsv(csvString: String): CsvImportReport = withContext(Dispatchers.IO) {
        var errorCount = 0
        val skippedItems = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var added = 0
        var skipped = 0

        try {
            val csvRecords = parseCsvRecords(csvString)

            if (csvRecords.isEmpty()) {
                onMessage(UiMessage.CsvEmpty)
                return@withContext CsvImportReport(CsvType.RECORDS, 0, 0, 1, emptyList(), listOf("CSV file is empty"))
            }

            // Sets already in history, legacy or v2, so an exported v2 set is not imported again as a duplicate.
            val zone = ZoneId.systemDefault()
            val history = CompatibilityHistory.read(database, zone)
            val existingKeys = history.map { listOf<Any>(it.exerciseId, it.date, it.time, it.setNumber) }.toHashSet()
            val existingMinutes = history.map { listOf<Any>(it.exerciseId, it.date, it.time) }.toHashSet()

            val rows = mutableListOf<CsvWorkoutImport.Row>()
            csvRecords.drop(1).forEachIndexed { index, columns ->
                val line = index + 2
                val row = parseRecordCsvRow(columns).getOrElse {
                    errors.add("Line $line: ${it.message}")
                    errorCount++
                    return@forEachIndexed
                }
                val exercise = exerciseDao.getExerciseByNameAndType(row.exerciseName, row.exerciseType)
                if (exercise == null) {
                    errors.add("Line $line: Exercise not found: ${row.exerciseName} (${row.exerciseType})")
                    errorCount++
                    return@forEachIndexed
                }
                rows += CsvWorkoutImport.Row(
                    line = line, exercise = exercise, date = row.date, time = row.time, setNumber = row.setNumber,
                    valueRight = row.valueRight, valueLeft = row.valueLeft, comment = row.comment,
                    distanceCm = row.distanceCm, weightG = row.weightG, assistanceG = row.assistanceG,
                    warmUp = row.warmUp, effortLeft = row.effortLeft,
                )
            }

            val plan = CsvWorkoutImport.plan(rows, database.backupDao().groups(), existingKeys, existingMinutes, zone, System.currentTimeMillis())
            plan.skipped.forEach {
                skippedItems.add("\"${it.exercise.name} (${it.exercise.type})\" - ${it.date} ${it.time} Set ${it.setNumber} (already exists)")
            }
            skipped = plan.skipped.size
            plan.refused.forEach {
                errors.add("Line ${it.line}: Not added: this workout is in the older record format, which cannot hold warm-up marks or effort")
                errorCount++
            }
            CsvWorkoutImport.write(database, plan)
            added = plan.added
        } catch (e: Exception) {
            onMessage(UiMessage.ImportError(e.message ?: ""))
        }

        return@withContext CsvImportReport(CsvType.RECORDS, added, skipped, errorCount, skippedItems, errors)
    }
}
