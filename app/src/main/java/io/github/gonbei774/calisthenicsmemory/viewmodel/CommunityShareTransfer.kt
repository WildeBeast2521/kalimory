package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Community share files: telling them from backups, exporting chosen programs, interval programs
 * and exercises with what they depend on, and previewing and importing a share. [onMessage]
 * receives what the screen should say; [appVersion] names the app version written into a share.
 */
class CommunityShareTransfer(
    private val database: AppDatabase,
    private val appVersion: () -> String,
    private val onMessage: (UiMessage) -> Unit,
) {
    private val exerciseDao = database.exerciseDao()
    private val groupDao = database.exerciseGroupDao()
    private val programDao = database.programDao()
    private val programExerciseDao = database.programExerciseDao()
    private val programLoopDao = database.programLoopDao()
    private val intervalProgramDao = database.intervalProgramDao()
    private val intervalProgramExerciseDao = database.intervalProgramExerciseDao()

    /**
     * JSON文字列のファイル種別を判定する
     * @return "backup", "share", "unknown"
     */
    fun detectJsonFileType(jsonString: String): String {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonElement = json.parseToJsonElement(jsonString)
            val jsonObject = jsonElement as? JsonObject ?: return "unknown"

            when {
                jsonObject.containsKey("exportType") -> {
                    val exportType = jsonObject["exportType"]
                    if (exportType is JsonPrimitive && exportType.content == "share") {
                        "share"
                    } else {
                        "unknown"
                    }
                }
                jsonObject.containsKey("version") && jsonObject.containsKey("app") -> "backup"
                else -> "unknown"
            }
        } catch (_: Exception) {
            "unknown"
        }
    }

    /**
     * 選択されたプログラム・インターバル・種目をコミュニティ共有用JSONにエクスポート
     */
    suspend fun exportCommunityShare(
        selectedProgramIds: Set<Long>,
        selectedIntervalProgramIds: Set<Long>,
        selectedExerciseIds: Set<Long>
    ): String = withContext(Dispatchers.IO) {
        try {
            // 全種目IDを集約（直接選択 + 依存種目）
            val allExerciseIds = selectedExerciseIds.toMutableSet()

            // プログラムの依存種目を収集
            val sharePrograms = mutableListOf<ShareProgram>()
            for (programId in selectedProgramIds) {
                val program = programDao.getProgramById(programId) ?: continue
                val programExercises = programExerciseDao.getExercisesForProgramSync(programId)
                val programLoops = programLoopDao.getLoopsForProgramSync(programId)

                // 依存種目IDを追加
                programExercises.forEach { pe -> allExerciseIds.add(pe.exerciseId) }

                // ループIDをローカル連番にマッピング
                val loopIdMap = mutableMapOf<Long, Int>()
                programLoops.forEachIndexed { index, loop ->
                    loopIdMap[loop.id] = index + 1
                }

                val shareLoops = programLoops.mapIndexed { index, loop ->
                    ShareProgramLoop(
                        id = index + 1,
                        sortOrder = loop.sortOrder,
                        rounds = loop.rounds,
                        restBetweenRounds = loop.restBetweenRounds
                    )
                }

                val shareProgramExercises = programExercises.map { pe ->
                    val exercise = exerciseDao.getExerciseById(pe.exerciseId)
                    ShareProgramExercise(
                        exerciseName = exercise?.name ?: "",
                        exerciseType = exercise?.type ?: "",
                        sortOrder = pe.sortOrder,
                        sets = pe.sets,
                        targetValue = pe.targetValue,
                        intervalSeconds = pe.intervalSeconds,
                        loopId = pe.loopId?.let { loopIdMap[it] }
                    )
                }

                sharePrograms.add(
                    ShareProgram(
                        name = program.name,
                        exercises = shareProgramExercises,
                        loops = shareLoops
                    )
                )
            }

            // インターバルプログラムの依存種目を収集
            val shareIntervalPrograms = mutableListOf<ShareIntervalProgram>()
            for (intervalId in selectedIntervalProgramIds) {
                val interval = intervalProgramDao.getProgramById(intervalId) ?: continue
                val intervalExercises = intervalProgramExerciseDao.getExercisesForProgramSync(intervalId)

                // 依存種目IDを追加
                intervalExercises.forEach { ie -> allExerciseIds.add(ie.exerciseId) }

                val shareIntervalExercises = intervalExercises.map { ie ->
                    val exercise = exerciseDao.getExerciseById(ie.exerciseId)
                    ShareIntervalProgramExercise(
                        exerciseName = exercise?.name ?: "",
                        exerciseType = exercise?.type ?: "",
                        sortOrder = ie.sortOrder
                    )
                }

                shareIntervalPrograms.add(
                    ShareIntervalProgram(
                        name = interval.name,
                        workSeconds = interval.workSeconds,
                        restSeconds = interval.restSeconds,
                        rounds = interval.rounds,
                        roundRestSeconds = interval.roundRestSeconds,
                        exercises = shareIntervalExercises
                    )
                )
            }

            // 種目を収集、重複排除
            val exerciseMap = mutableMapOf<Long, ShareExercise>()
            val groupNames = mutableSetOf<String>()
            for (exerciseId in allExerciseIds) {
                val exercise = exerciseDao.getExerciseById(exerciseId) ?: continue
                exerciseMap[exerciseId] = ShareExercise(
                    name = exercise.name,
                    type = exercise.type,
                    group = exercise.group,
                    sortOrder = exercise.sortOrder,
                    laterality = exercise.laterality,
                    targetSets = exercise.targetSets,
                    targetValue = exercise.targetValue,
                    restInterval = exercise.restInterval,
                    repDuration = exercise.repDuration,
                    distanceTrackingEnabled = exercise.distanceTrackingEnabled,
                    weightTrackingEnabled = exercise.weightTrackingEnabled,
                    assistanceTrackingEnabled = exercise.assistanceTrackingEnabled,
                    description = exercise.description,
                    catalogId = exercise.catalogId
                )
                exercise.group?.let { groupNames.add(it) }
            }

            val shareGroups = groupNames.sorted().map { ShareGroup(name = it) }

            // アプリバージョン取得
            val appVersion = appVersion()

            val shareData = CommunityShareData(
                formatVersion = 1,
                exportType = "share",
                exportDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                exportId = java.util.UUID.randomUUID().toString(),
                appVersion = appVersion,
                data = CommunityShareContent(
                    groups = shareGroups,
                    exercises = exerciseMap.values.toList(),
                    programs = sharePrograms,
                    intervalPrograms = shareIntervalPrograms
                )
            )

            onMessage(
                UiMessage.CommunityShareExportComplete(
                    exerciseCount = exerciseMap.size,
                    programCount = sharePrograms.size,
                    intervalProgramCount = shareIntervalPrograms.size,
                )
            )

            val json = Json { ignoreUnknownKeys = true }
            json.encodeToString(shareData)
        } catch (e: Exception) {
            onMessage(UiMessage.ExportError(e.message ?: ""))
            throw e
        }
    }

    /**
     * コミュニティ共有JSONをインポート
     */
    suspend fun previewCommunityShareImport(data: CommunityShareData): CommunityShareImportReport = withContext(Dispatchers.IO) {
        val content = data.data
        var groupsAdded = 0
        var groupsReused = 0
        var exercisesAdded = 0
        var exercisesSkipped = 0
        var programsAdded = 0
        var programsSkipped = 0
        var intervalProgramsAdded = 0
        var intervalProgramsSkipped = 0

        // グループ
        for (shareGroup in content.groups) {
            val existing = groupDao.getGroupByName(shareGroup.name)
            if (existing != null) groupsReused++ else groupsAdded++
        }

        // 種目
        val seenExerciseKeys = mutableSetOf<String>()
        for (shareExercise in content.exercises) {
            val key = "${shareExercise.name}|${shareExercise.type}"
            if (!seenExerciseKeys.add(key)) continue
            val existing = exerciseDao.getExerciseByNameAndType(shareExercise.name, shareExercise.type)
            if (existing != null) exercisesSkipped++ else exercisesAdded++
        }

        // プログラム
        for (shareProgram in content.programs) {
            val existing = programDao.getProgramByName(shareProgram.name)
            if (existing != null) programsSkipped++ else programsAdded++
        }

        // インターバル
        for (shareInterval in content.intervalPrograms) {
            val existing = intervalProgramDao.getProgramByName(shareInterval.name)
            if (existing != null) intervalProgramsSkipped++ else intervalProgramsAdded++
        }

        CommunityShareImportReport(
            groupsAdded = groupsAdded,
            groupsReused = groupsReused,
            exercisesAdded = exercisesAdded,
            exercisesSkipped = exercisesSkipped,
            programsAdded = programsAdded,
            programsSkipped = programsSkipped,
            intervalProgramsAdded = intervalProgramsAdded,
            intervalProgramsSkipped = intervalProgramsSkipped
        )
    }

    suspend fun importCommunityShare(jsonString: String): CommunityShareImportReport = withContext(Dispatchers.IO) {
        try {
            // ファイル種別チェック: バックアップJSONが渡された場合はエラー
            if (detectJsonFileType(jsonString) == "backup") {
                onMessage(UiMessage.WrongFileType(detected = "backup", expected = "share"))
                return@withContext CommunityShareImportReport(
                    errors = listOf("Wrong file type: backup")
                )
            }

            val json = Json { ignoreUnknownKeys = true }
            val shareData = json.decodeFromString<CommunityShareData>(jsonString)

            // バリデーション
            val validationErrors = validateCommunityShareContent(shareData)
            if (validationErrors.isNotEmpty()) {
                onMessage(UiMessage.CommunityShareImportError(validationErrors.first()))
                return@withContext CommunityShareImportReport(errors = validationErrors)
            }

            val report = CommunityShareImporter(database).import(shareData.data)

            onMessage(UiMessage.CommunityShareImportComplete(report))

            report
        } catch (e: kotlinx.serialization.SerializationException) {
            onMessage(UiMessage.CommunityShareImportError(e.message ?: "Invalid JSON format"))
            CommunityShareImportReport(errors = listOf(e.message ?: "Invalid JSON format"))
        } catch (e: Exception) {
            onMessage(UiMessage.CommunityShareImportError(e.message ?: "Import failed"))
            CommunityShareImportReport(errors = listOf(e.message ?: "Import failed"))
        }
    }
}
