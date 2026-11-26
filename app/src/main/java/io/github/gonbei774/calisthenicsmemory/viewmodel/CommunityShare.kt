package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.R
import kotlinx.serialization.Serializable

// ===== コミュニティ共有用シリアライゼーションモデル =====

@Serializable
data class CommunityShareData(
    val formatVersion: Int,
    val exportType: String,
    val exportDate: String,
    val exportId: String,
    val appVersion: String,
    val data: CommunityShareContent
)

@Serializable
data class CommunityShareContent(
    val groups: List<ShareGroup> = emptyList(),
    val exercises: List<ShareExercise> = emptyList(),
    val programs: List<ShareProgram> = emptyList(),
    val intervalPrograms: List<ShareIntervalProgram> = emptyList()
)

@Serializable
data class ShareGroup(
    val name: String
)

@Serializable
data class ShareExercise(
    val name: String,
    val type: String,
    val group: String? = null,
    val sortOrder: Int = 0,
    val laterality: String = "Bilateral",
    val targetSets: Int? = null,
    val targetValue: Int? = null,
    val restInterval: Int? = null,
    val repDuration: Int? = null,
    val distanceTrackingEnabled: Boolean = false,
    val weightTrackingEnabled: Boolean = false,
    val assistanceTrackingEnabled: Boolean = false,
    val description: String? = null,
    /** The catalogue step it is linked to (ADR 0007); older apps ignore it. */
    val catalogId: String? = null,
    /** The muscles chosen for an own exercise, as muscle codes (ADR 0014); older apps ignore them. */
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    /** The share of body weight of a rep exercise, in whole percent (ADR 0016); the weights themselves are never shared. */
    val bodyWeightPercent: Int? = null
)

@Serializable
data class ShareProgram(
    val name: String,
    val exercises: List<ShareProgramExercise> = emptyList(),
    val loops: List<ShareProgramLoop> = emptyList()
)

@Serializable
data class ShareProgramExercise(
    val exerciseName: String,
    val exerciseType: String,
    val sortOrder: Int,
    val sets: Int = 1,
    val targetValue: Int = 0,
    val intervalSeconds: Int = 60,
    val loopId: Int? = null
)

@Serializable
data class ShareProgramLoop(
    val id: Int,
    val sortOrder: Int,
    val rounds: Int,
    val restBetweenRounds: Int
)

@Serializable
data class ShareIntervalProgram(
    val name: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val roundRestSeconds: Int,
    val exercises: List<ShareIntervalProgramExercise> = emptyList()
)

@Serializable
data class ShareIntervalProgramExercise(
    val exerciseName: String,
    val exerciseType: String,
    val sortOrder: Int
)

// ===== インポートレポート =====

data class CommunityShareImportReport(
    val groupsAdded: Int = 0,
    val groupsReused: Int = 0,
    val exercisesAdded: Int = 0,
    val exercisesSkipped: Int = 0,
    val programsAdded: Int = 0,
    val programsSkipped: Int = 0,
    val intervalProgramsAdded: Int = 0,
    val intervalProgramsSkipped: Int = 0,
    val errors: List<ImportIssue> = emptyList()
)

// ===== バリデーション =====

private const val CURRENT_FORMAT_VERSION = 1
private const val MAX_NAME_LENGTH = 100
private const val MAX_DESCRIPTION_LENGTH = 60
private const val MAX_NUMERIC_VALUE = 999
private val VALID_EXERCISE_TYPES = setOf("Dynamic", "Isometric")
private val VALID_LATERALITY = setOf("Bilateral", "Unilateral")
internal const val MAX_CATALOG_ID_LENGTH = 64
internal val CATALOG_ID_PATTERN = Regex("^[a-z0-9_]+\\.[a-z0-9_]+$")

fun validateCommunityShareContent(data: CommunityShareData): List<ImportIssue> {
    val errors = mutableListOf<ImportIssue>()

    // フォーマットバージョンチェック
    if (data.formatVersion > CURRENT_FORMAT_VERSION) {
        errors.add(ImportIssue(R.string.share_issue_version, listOf(data.formatVersion)))
        return errors
    }

    // exportType チェック
    if (data.exportType != "share") {
        errors.add(ImportIssue(R.string.share_issue_export_type, listOf(data.exportType)))
        return errors
    }

    val content = data.data

    // グループのバリデーション
    content.groups.forEachIndexed { index, group ->
        if (group.name.isBlank()) {
            errors.add(ImportIssue(R.string.share_issue_name_empty, listOf("groups[$index]")))
        }
        if (group.name.length > MAX_NAME_LENGTH) {
            errors.add(ImportIssue(R.string.share_issue_name_long, listOf("groups[$index]", MAX_NAME_LENGTH)))
        }
    }

    // 種目のバリデーション
    val exerciseKeys = mutableSetOf<String>()
    val catalogIds = mutableSetOf<String>()
    content.exercises.forEachIndexed { index, exercise ->
        if (exercise.name.isBlank()) {
            errors.add(ImportIssue(R.string.share_issue_name_empty, listOf("exercises[$index]")))
        }
        if (exercise.name.length > MAX_NAME_LENGTH) {
            errors.add(ImportIssue(R.string.share_issue_name_long, listOf("exercises[$index]", MAX_NAME_LENGTH)))
        }
        if (exercise.type !in VALID_EXERCISE_TYPES) {
            errors.add(ImportIssue(R.string.share_issue_invalid_type, listOf("exercises[$index]", "${exercise.type}")))
        }
        if (exercise.laterality !in VALID_LATERALITY) {
            errors.add(ImportIssue(R.string.share_issue_invalid_laterality, listOf("exercises[$index]", "${exercise.laterality}")))
        }
        exercise.targetSets?.let {
            if (it <= 0 || it > MAX_NUMERIC_VALUE) {
                errors.add(ImportIssue(R.string.share_issue_range, listOf("exercises[$index]", "targetSets", 1, MAX_NUMERIC_VALUE)))
            }
        }
        exercise.targetValue?.let {
            if (it <= 0 || it > MAX_NUMERIC_VALUE) {
                errors.add(ImportIssue(R.string.share_issue_range, listOf("exercises[$index]", "targetValue", 1, MAX_NUMERIC_VALUE)))
            }
        }
        exercise.restInterval?.let {
            if (it < 0 || it > MAX_NUMERIC_VALUE) {
                errors.add(ImportIssue(R.string.share_issue_range, listOf("exercises[$index]", "restInterval", 0, MAX_NUMERIC_VALUE)))
            }
        }
        exercise.repDuration?.let {
            if (it <= 0 || it > MAX_NUMERIC_VALUE) {
                errors.add(ImportIssue(R.string.share_issue_range, listOf("exercises[$index]", "repDuration", 1, MAX_NUMERIC_VALUE)))
            }
        }
        // A step id from a newer catalogue is kept; a malformed or repeated one is not.
        exercise.catalogId?.let {
            if (it.length > MAX_CATALOG_ID_LENGTH || !CATALOG_ID_PATTERN.matches(it)) {
                errors.add(ImportIssue(R.string.share_issue_invalid_catalog, listOf("exercises[$index]", "$it")))
            } else if (!catalogIds.add(it)) {
                errors.add(ImportIssue(R.string.share_issue_dup_catalog, listOf("exercises[$index]", "$it")))
            }
        }
        exercise.bodyWeightPercent?.takeIf { it !in 1..100 }?.let {
            errors.add(ImportIssue(R.string.share_issue_body_weight_percent, listOf("exercises[$index]", it)))
        }
        (exercise.primaryMuscles + exercise.secondaryMuscles).firstOrNull { code -> Muscle.entries.none { it.code == code } }?.let { code ->
            errors.add(ImportIssue(R.string.share_issue_unknown_muscle, listOf("exercises[$index]", code)))
        }
        // グループ参照チェック
        exercise.group?.let { groupName ->
            if (content.groups.none { it.name == groupName }) {
                errors.add(ImportIssue(R.string.share_issue_unknown_group, listOf("exercises[$index]", "$groupName")))
            }
        }
        // 重複チェック（先勝ち）
        val key = "${exercise.name}|${exercise.type}"
        if (!exerciseKeys.add(key)) {
            errors.add(ImportIssue(R.string.share_issue_dup_exercise, listOf("exercises[$index]", "${exercise.name} (${exercise.type})")))
        }
    }

    // プログラムのバリデーション
    val programNames = mutableSetOf<String>()
    content.programs.forEachIndexed { pIndex, program ->
        if (program.name.isBlank()) {
            errors.add(ImportIssue(R.string.share_issue_name_empty, listOf("programs[$pIndex]")))
        }
        if (program.name.length > MAX_NAME_LENGTH) {
            errors.add(ImportIssue(R.string.share_issue_name_long, listOf("programs[$pIndex]", MAX_NAME_LENGTH)))
        }
        if (!programNames.add(program.name)) {
            errors.add(ImportIssue(R.string.share_issue_dup_program, listOf("programs[$pIndex]", "${program.name}")))
        }

        // ループのバリデーション
        val loopIds = mutableSetOf<Int>()
        program.loops.forEachIndexed { lIndex, loop ->
            if (!loopIds.add(loop.id)) {
                errors.add(ImportIssue(R.string.share_issue_dup_loop, listOf("programs[$pIndex].loops[$lIndex]", "${loop.id}")))
            }
            if (loop.rounds <= 0 || loop.rounds > MAX_NUMERIC_VALUE) {
                errors.add(ImportIssue(R.string.share_issue_range, listOf("programs[$pIndex].loops[$lIndex]", "rounds", 1, MAX_NUMERIC_VALUE)))
            }
            if (loop.restBetweenRounds < 0 || loop.restBetweenRounds > MAX_NUMERIC_VALUE) {
                errors.add(ImportIssue(R.string.share_issue_range, listOf("programs[$pIndex].loops[$lIndex]", "restBetweenRounds", 0, MAX_NUMERIC_VALUE)))
            }
        }

        // プログラム内種目のバリデーション
        program.exercises.forEachIndexed { eIndex, pe ->
            val key = "${pe.exerciseName}|${pe.exerciseType}"
            if (key !in exerciseKeys) {
                errors.add(ImportIssue(R.string.share_issue_unknown_exercise, listOf("programs[$pIndex].exercises[$eIndex]", "${pe.exerciseName} (${pe.exerciseType})")))
            }
            if (pe.sets <= 0 || pe.sets > MAX_NUMERIC_VALUE) {
                errors.add(ImportIssue(R.string.share_issue_range, listOf("programs[$pIndex].exercises[$eIndex]", "sets", 1, MAX_NUMERIC_VALUE)))
            }
            if (pe.targetValue < 0 || pe.targetValue > MAX_NUMERIC_VALUE) {
                errors.add(ImportIssue(R.string.share_issue_range, listOf("programs[$pIndex].exercises[$eIndex]", "targetValue", 0, MAX_NUMERIC_VALUE)))
            }
            if (pe.intervalSeconds < 0 || pe.intervalSeconds > MAX_NUMERIC_VALUE) {
                errors.add(ImportIssue(R.string.share_issue_range, listOf("programs[$pIndex].exercises[$eIndex]", "intervalSeconds", 0, MAX_NUMERIC_VALUE)))
            }
            pe.loopId?.let { lid ->
                if (lid !in loopIds) {
                    errors.add(ImportIssue(R.string.share_issue_unknown_loop, listOf("programs[$pIndex].exercises[$eIndex]", "$lid")))
                }
            }
        }
    }

    // インターバルプログラムのバリデーション
    val intervalNames = mutableSetOf<String>()
    content.intervalPrograms.forEachIndexed { iIndex, interval ->
        if (interval.name.isBlank()) {
            errors.add(ImportIssue(R.string.share_issue_name_empty, listOf("intervalPrograms[$iIndex]")))
        }
        if (interval.name.length > MAX_NAME_LENGTH) {
            errors.add(ImportIssue(R.string.share_issue_name_long, listOf("intervalPrograms[$iIndex]", MAX_NAME_LENGTH)))
        }
        if (!intervalNames.add(interval.name)) {
            errors.add(ImportIssue(R.string.share_issue_dup_interval, listOf("intervalPrograms[$iIndex]", "${interval.name}")))
        }
        if (interval.workSeconds <= 0 || interval.workSeconds > MAX_NUMERIC_VALUE) {
            errors.add(ImportIssue(R.string.share_issue_range, listOf("intervalPrograms[$iIndex]", "workSeconds", 1, MAX_NUMERIC_VALUE)))
        }
        if (interval.restSeconds < 0 || interval.restSeconds > MAX_NUMERIC_VALUE) {
            errors.add(ImportIssue(R.string.share_issue_range, listOf("intervalPrograms[$iIndex]", "restSeconds", 0, MAX_NUMERIC_VALUE)))
        }
        if (interval.rounds <= 0 || interval.rounds > MAX_NUMERIC_VALUE) {
            errors.add(ImportIssue(R.string.share_issue_range, listOf("intervalPrograms[$iIndex]", "rounds", 1, MAX_NUMERIC_VALUE)))
        }
        if (interval.roundRestSeconds < 0 || interval.roundRestSeconds > MAX_NUMERIC_VALUE) {
            errors.add(ImportIssue(R.string.share_issue_range, listOf("intervalPrograms[$iIndex]", "roundRestSeconds", 0, MAX_NUMERIC_VALUE)))
        }

        // インターバル内種目のバリデーション
        interval.exercises.forEachIndexed { eIndex, ie ->
            val key = "${ie.exerciseName}|${ie.exerciseType}"
            if (key !in exerciseKeys) {
                errors.add(ImportIssue(R.string.share_issue_unknown_exercise, listOf("intervalPrograms[$iIndex].exercises[$eIndex]", "${ie.exerciseName} (${ie.exerciseType})")))
            }
        }
    }

    return errors
}
