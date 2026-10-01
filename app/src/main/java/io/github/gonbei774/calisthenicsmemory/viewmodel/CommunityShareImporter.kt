package io.github.gonbei774.calisthenicsmemory.viewmodel

import androidx.room.withTransaction
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop

/**
 * Writes validated community-share content in one Room transaction, so a failure
 * part-way through (including cancellation or process death) leaves no partial import.
 */
class CommunityShareImporter(private val database: AppDatabase) {
    suspend fun import(content: CommunityShareContent): CommunityShareImportReport =
        database.withTransaction {
            var groupsAdded = 0
            var groupsReused = 0
            var exercisesAdded = 0
            var exercisesSkipped = 0
            var programsAdded = 0
            var programsSkipped = 0
            var intervalProgramsAdded = 0
            var intervalProgramsSkipped = 0

            // displayOrder の現在最大値を取得
            var nextDisplayOrder = database.exerciseDao().getMaxDisplayOrder() + 1

            // 1. グループをインポート
            for (shareGroup in content.groups) {
                val existing = database.exerciseGroupDao().getGroupByName(shareGroup.name)
                if (existing != null) {
                    groupsReused++
                } else {
                    database.exerciseGroupDao().insertGroup(ExerciseGroup(name = shareGroup.name))
                    groupsAdded++
                }
            }

            // 2. 種目をインポート（名前+タイプでマッチ、重複はスキップ）
            // exerciseIdMap: "name|type" -> DB ID
            val exerciseIdMap = mutableMapOf<String, Long>()
            val seenExerciseKeys = mutableSetOf<String>()
            for (shareExercise in content.exercises) {
                val key = "${shareExercise.name}|${shareExercise.type}"
                // JSON内重複は先勝ち
                if (!seenExerciseKeys.add(key)) continue

                val existing = database.exerciseDao().getExerciseByNameAndType(shareExercise.name, shareExercise.type)
                if (existing != null) {
                    exerciseIdMap[key] = existing.id
                    exercisesSkipped++
                } else {
                    val newExercise = Exercise(
                        name = shareExercise.name,
                        type = shareExercise.type,
                        group = shareExercise.group,
                        sortOrder = shareExercise.sortOrder,
                        displayOrder = nextDisplayOrder++,
                        laterality = shareExercise.laterality,
                        targetSets = shareExercise.targetSets,
                        targetValue = shareExercise.targetValue,
                        restInterval = shareExercise.restInterval,
                        repDuration = shareExercise.repDuration,
                        distanceTrackingEnabled = shareExercise.distanceTrackingEnabled,
                        weightTrackingEnabled = shareExercise.weightTrackingEnabled,
                        assistanceTrackingEnabled = shareExercise.assistanceTrackingEnabled,
                        description = shareExercise.description?.take(120),
                        // Linked only when no exercise here holds that step yet; existing ones are left as they are.
                        catalogId = shareExercise.catalogId?.takeIf { database.exerciseDao().getExerciseByCatalogId(it) == null }
                    )
                    val newId = database.exerciseDao().insertExercise(newExercise)
                    exerciseIdMap[key] = newId
                    exercisesAdded++
                }
            }

            // 3. プログラムをインポート
            for (shareProgram in content.programs) {
                val existing = database.programDao().getProgramByName(shareProgram.name)
                if (existing != null) {
                    programsSkipped++
                    continue
                }

                // プログラム作成
                val newProgramId = database.programDao().insert(Program(name = shareProgram.name))

                // ループ作成（ローカルID → DB IDマッピング）
                val loopIdMap = mutableMapOf<Int, Long>()
                for (shareLoop in shareProgram.loops) {
                    val newLoopId = database.programLoopDao().insert(
                        ProgramLoop(
                            programId = newProgramId,
                            sortOrder = shareLoop.sortOrder,
                            rounds = shareLoop.rounds,
                            restBetweenRounds = shareLoop.restBetweenRounds
                        )
                    )
                    loopIdMap[shareLoop.id] = newLoopId
                }

                // ProgramExercise作成
                for (sharePe in shareProgram.exercises) {
                    val exerciseKey = "${sharePe.exerciseName}|${sharePe.exerciseType}"
                    val exerciseId = exerciseIdMap[exerciseKey] ?: continue
                    database.programExerciseDao().insert(
                        ProgramExercise(
                            programId = newProgramId,
                            exerciseId = exerciseId,
                            sortOrder = sharePe.sortOrder,
                            sets = sharePe.sets,
                            targetValue = sharePe.targetValue,
                            intervalSeconds = sharePe.intervalSeconds,
                            loopId = sharePe.loopId?.let { loopIdMap[it] }
                        )
                    )
                }

                programsAdded++
            }

            // 4. インターバルプログラムをインポート
            for (shareInterval in content.intervalPrograms) {
                val existing = database.intervalProgramDao().getProgramByName(shareInterval.name)
                if (existing != null) {
                    intervalProgramsSkipped++
                    continue
                }

                // インターバルプログラム作成
                val newIntervalId = database.intervalProgramDao().insert(
                    IntervalProgram(
                        name = shareInterval.name,
                        workSeconds = shareInterval.workSeconds,
                        restSeconds = shareInterval.restSeconds,
                        rounds = shareInterval.rounds,
                        roundRestSeconds = shareInterval.roundRestSeconds
                    )
                )

                // IntervalProgramExercise作成
                for (shareIe in shareInterval.exercises) {
                    val exerciseKey = "${shareIe.exerciseName}|${shareIe.exerciseType}"
                    val exerciseId = exerciseIdMap[exerciseKey] ?: continue
                    database.intervalProgramExerciseDao().insert(
                        IntervalProgramExercise(
                            programId = newIntervalId,
                            exerciseId = exerciseId,
                            sortOrder = shareIe.sortOrder
                        )
                    )
                }

                intervalProgramsAdded++
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
}
