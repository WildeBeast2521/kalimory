package io.github.gonbei774.calisthenicsmemory.viewmodel

import kotlinx.serialization.Serializable

/*
 * The JSON backup's wire format (BackupService.CURRENT_VERSION). Field names and defaults are the
 * file format: older backups omit newer fields, so every field added later has a default.
 */

// バックアップ用データクラス
@Serializable
data class BackupData(
    val version: Int,
    val exportDate: String,
    val app: String,
    val groups: List<ExportGroup>,
    val exercises: List<ExportExercise>,
    val records: List<ExportRecord>,
    val programs: List<ExportProgram> = emptyList(),           // v4で追加
    val programExercises: List<ExportProgramExercise> = emptyList(),  // v4で追加
    val programLoops: List<ExportProgramLoop> = emptyList(),    // v5で追加（後方互換性のためデフォルト空）
    val intervalPrograms: List<ExportIntervalProgram> = emptyList(),              // v7で追加
    val intervalProgramExercises: List<ExportIntervalProgramExercise> = emptyList(), // v7で追加
    val intervalRecords: List<ExportIntervalRecord> = emptyList(),                 // v7で追加
    val todoTasks: List<ExportTodoTask> = emptyList(),                            // v8で追加
    val workoutSessions: List<ExportWorkoutSession> = emptyList(),                // v9で追加（v2 履歴）
    val sessionExercises: List<ExportSessionExercise> = emptyList(),              // v9で追加
    val setEntries: List<ExportSetEntry> = emptyList(),                           // v9で追加
    val chainPlacements: List<ExportChainPlacement> = emptyList(),                // v11: custom exercises in built-in chains
)

/** Format 11: a custom exercise placed in a built-in chain (ADR 0007). */
@Serializable
data class ExportChainPlacement(
    val exerciseId: Long,
    val chainId: String,
    val afterStepId: String? = null,
)

@Serializable
data class ExportGroup(
    val id: Long,
    val name: String,
    val displayOrder: Int = 0
)

@Serializable
data class ExportExercise(
    val id: Long,
    val name: String,
    val type: String,
    val group: String?,
    val sortOrder: Int,
    val displayOrder: Int = 0,       // 表示順（デフォルト値で後方互換）
    val laterality: String,
    val targetSets: Int? = null,
    val targetValue: Int? = null,
    val isFavorite: Boolean = false, // お気に入り（デフォルト値で後方互換）
    val restInterval: Int? = null,   // 種目固有の休憩時間（デフォルト値で後方互換）
    val repDuration: Int? = null,    // 種目固有の1レップ時間（デフォルト値で後方互換）
    val distanceTrackingEnabled: Boolean = false,  // 距離入力を有効化（v3で追加）
    val weightTrackingEnabled: Boolean = false,    // 荷重入力を有効化（v3で追加）
    val assistanceTrackingEnabled: Boolean = false, // アシスト入力を有効化（v6で追加）
    val description: String? = null,               // 種目の説明文（v6で追加）
    val catalogId: String? = null,                 // Format 11: the catalogue step it was added from (ADR 0007)
)

@Serializable
data class ExportRecord(
    val id: Long,
    val exerciseId: Long,
    val valueRight: Int,
    val valueLeft: Int?,
    val setNumber: Int,
    val date: String,
    val time: String,
    val comment: String,
    val distanceCm: Int? = null,  // 距離（cm、v3で追加）
    val weightG: Int? = null,     // 追加ウエイト（g、v3で追加）
    val assistanceG: Int? = null  // アシスト量（g、v6で追加）
)

@Serializable
data class ExportProgram(
    val id: Long,
    val name: String
    // timerMode/startIntervalはSharedPreferencesへ移行（v14）
    // 旧JSONインポート時はignoreUnknownKeysで無視される
)

@Serializable
data class ExportProgramExercise(
    val id: Long,
    val programId: Long,
    val exerciseId: Long,
    val sortOrder: Int,
    val sets: Int,
    val targetValue: Int,
    val intervalSeconds: Int,
    val loopId: Long? = null  // v5で追加（後方互換性のためデフォルトnull）
)

@Serializable
data class ExportProgramLoop(
    val id: Long,
    val programId: Long,
    val sortOrder: Int,
    val rounds: Int,
    val restBetweenRounds: Int
)

@Serializable
data class ExportIntervalProgram(
    val id: Long,
    val name: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val roundRestSeconds: Int
)

@Serializable
data class ExportIntervalProgramExercise(
    val id: Long,
    val programId: Long,
    val exerciseId: Long,
    val sortOrder: Int
)

@Serializable
data class ExportIntervalRecord(
    val id: Long,
    val programName: String,
    val date: String,
    val time: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val roundRestSeconds: Int,
    val completedRounds: Int,
    val completedExercisesInLastRound: Int,
    val exercisesJson: String,
    val comment: String? = null
)

@Serializable
data class ExportTodoTask(
    val id: Long,
    val type: String,
    val referenceId: Long,
    val sortOrder: Int,
    val repeatDays: String = "",
    val lastCompletedDate: String? = null
)
