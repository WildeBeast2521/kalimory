// AppDatabase.kt
package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.gonbei774.calisthenicsmemory.data.v2.SessionExerciseEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionDao
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSessionEntity
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutTypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Exercise::class, TrainingRecord::class, ExerciseGroup::class, TodoTask::class, Program::class, ProgramExercise::class, ProgramLoop::class, IntervalProgram::class, IntervalProgramExercise::class, IntervalRecord::class, WorkoutSessionEntity::class, SessionExerciseEntity::class, SetEntryEntity::class],
    version = 24,
    exportSchema = true
)
@TypeConverters(WorkoutTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun trainingRecordDao(): TrainingRecordDao
    abstract fun exerciseGroupDao(): ExerciseGroupDao
    abstract fun todoTaskDao(): TodoTaskDao
    abstract fun programDao(): ProgramDao
    abstract fun programExerciseDao(): ProgramExerciseDao
    abstract fun programLoopDao(): ProgramLoopDao
    abstract fun intervalProgramDao(): IntervalProgramDao
    abstract fun intervalProgramExerciseDao(): IntervalProgramExerciseDao
    abstract fun intervalRecordDao(): IntervalRecordDao
    abstract fun backupDao(): BackupDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = build(context, DATABASE_NAME)
                INSTANCE = instance
                instance
            }
        }

        const val DATABASE_NAME = "bodyweight_trainer_database"

        // Must equal the version in @Database; MigrationRegistrationTest checks it against the schemas.
        const val CURRENT_VERSION = 24

        // The production configuration. Migration tests open their databases through it.
        // Unsupported installed versions are refused before Room can modify the file, and
        // SQLite corruption keeps the file instead of deleting it.
        internal fun build(context: Context, name: String): AppDatabase {
            InstalledDatabaseVersion.requireSupported(
                context.applicationContext.getDatabasePath(name),
                OLDEST_SUPPORTED_VERSION..CURRENT_VERSION,
            )
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, name)
                .addMigrations(*ALL_MIGRATIONS)
                .openHelperFactory(CorruptionPreservingOpenHelperFactory())
                .build()
        }

        // マイグレーション 9 → 10: displayOrder, restInterval, repDuration を追加
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. displayOrder フィールドを追加（並び替え機能用）
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0"
                )

                // 2. restInterval フィールドを追加（タイマー設定機能用）
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN restInterval INTEGER"
                )

                // 3. repDuration フィールドを追加（タイマー設定機能用）
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN repDuration INTEGER"
                )

                // 4. displayOrder の初期値を設定
                //    グループ内で sortOrder の昇順に従って 0, 1, 2... を割り当て
                //    グループ外は name の昇順で連番を割り当て

                // グループ内の種目に連番を割り当て
                database.execSQL("""
                    UPDATE exercises
                    SET displayOrder = (
                        SELECT COUNT(*)
                        FROM exercises e2
                        WHERE e2.`group` IS NOT NULL
                        AND e2.`group` = exercises.`group`
                        AND (
                            e2.sortOrder < exercises.sortOrder
                            OR (e2.sortOrder = exercises.sortOrder AND e2.id < exercises.id)
                        )
                    )
                    WHERE exercises.`group` IS NOT NULL
                """)

                // グループ外の種目に連番を割り当て（名前順）
                database.execSQL("""
                    UPDATE exercises
                    SET displayOrder = (
                        SELECT COUNT(*)
                        FROM exercises e2
                        WHERE e2.`group` IS NULL
                        AND e2.name < exercises.name
                    )
                    WHERE exercises.`group` IS NULL
                """)
            }
        }

        // マイグレーション 10 → 11: 距離・荷重トラッキング機能を追加
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Exercise テーブルに距離・荷重トラッキングフラグを追加
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN distanceTrackingEnabled INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN weightTrackingEnabled INTEGER NOT NULL DEFAULT 0"
                )

                // TrainingRecord テーブルに距離・荷重値を追加
                database.execSQL(
                    "ALTER TABLE training_records ADD COLUMN distanceCm INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE training_records ADD COLUMN weightG INTEGER"
                )
            }
        }

        // マイグレーション 11 → 12: To Do機能（TodoTaskテーブル追加）
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS todo_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                """)
            }
        }

        // マイグレーション 12 → 13: Program機能（プログラム・種目テーブル追加）
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Program テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS programs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        timerMode INTEGER NOT NULL DEFAULT 0,
                        startInterval INTEGER NOT NULL DEFAULT 5
                    )
                """)

                // ProgramExercise テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS program_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        programId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        sets INTEGER NOT NULL DEFAULT 1,
                        targetValue INTEGER NOT NULL,
                        intervalSeconds INTEGER NOT NULL DEFAULT 60,
                        FOREIGN KEY (programId) REFERENCES programs(id) ON DELETE CASCADE,
                        FOREIGN KEY (exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                """)

                // インデックス作成
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_exercises_programId ON program_exercises(programId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_exercises_exerciseId ON program_exercises(exerciseId)")
            }
        }

        // マイグレーション 13 → 14: timerMode/startIntervalをSharedPreferencesへ移行
        // これらは「ユーザーの好み」であり「プログラムのコンテンツ」ではないため
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQLiteではカラム削除が直接できないため、テーブル再作成が必要
                // 1. 新テーブル作成（timerMode, startIntervalなし）
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS programs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """)

                // 2. データコピー（id, nameのみ）
                database.execSQL("""
                    INSERT INTO programs_new (id, name)
                    SELECT id, name FROM programs
                """)

                // 3. 旧テーブル削除
                database.execSQL("DROP TABLE programs")

                // 4. 新テーブルをリネーム
                database.execSQL("ALTER TABLE programs_new RENAME TO programs")
            }
        }

        // マイグレーション 14 → 15: ループ機能追加
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. ProgramLoop テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS program_loops (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        programId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        rounds INTEGER NOT NULL,
                        restBetweenRounds INTEGER NOT NULL,
                        FOREIGN KEY (programId) REFERENCES programs(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_loops_programId ON program_loops(programId)")

                // 2. ProgramExercise テーブル再作成（ForeignKey追加のため）
                // SQLiteではALTER TABLEでForeignKeyを追加できないため、テーブル再作成が必要
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS program_exercises_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        programId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        sets INTEGER NOT NULL,
                        targetValue INTEGER NOT NULL,
                        intervalSeconds INTEGER NOT NULL,
                        loopId INTEGER DEFAULT NULL,
                        FOREIGN KEY (programId) REFERENCES programs(id) ON DELETE CASCADE,
                        FOREIGN KEY (exerciseId) REFERENCES exercises(id) ON DELETE CASCADE,
                        FOREIGN KEY (loopId) REFERENCES program_loops(id) ON DELETE CASCADE
                    )
                """)

                // データコピー
                database.execSQL("""
                    INSERT INTO program_exercises_new (id, programId, exerciseId, sortOrder, sets, targetValue, intervalSeconds, loopId)
                    SELECT id, programId, exerciseId, sortOrder, sets, targetValue, intervalSeconds, NULL
                    FROM program_exercises
                """)

                // 旧テーブル削除
                database.execSQL("DROP TABLE program_exercises")

                // 新テーブルをリネーム
                database.execSQL("ALTER TABLE program_exercises_new RENAME TO program_exercises")

                // インデックス作成
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_exercises_programId ON program_exercises(programId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_exercises_exerciseId ON program_exercises(exerciseId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_program_exercises_loopId ON program_exercises(loopId)")
            }
        }

        // マイグレーション 15 → 16: アシストトラッキング機能を追加
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Exercise テーブルにアシストトラッキングフラグを追加
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN assistanceTrackingEnabled INTEGER NOT NULL DEFAULT 0"
                )

                // TrainingRecord テーブルにアシスト値を追加
                database.execSQL(
                    "ALTER TABLE training_records ADD COLUMN assistanceG INTEGER"
                )
            }
        }

        // マイグレーション 16 → 17: 種目の説明文フィールドを追加
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN description TEXT"
                )
            }
        }

        // マイグレーション 17 → 18: インターバルモード用テーブル追加
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // IntervalProgram テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS interval_programs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        workSeconds INTEGER NOT NULL,
                        restSeconds INTEGER NOT NULL,
                        rounds INTEGER NOT NULL,
                        roundRestSeconds INTEGER NOT NULL
                    )
                """)

                // IntervalProgramExercise テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS interval_program_exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        programId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY (programId) REFERENCES interval_programs(id) ON DELETE CASCADE,
                        FOREIGN KEY (exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_interval_program_exercises_programId ON interval_program_exercises(programId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_interval_program_exercises_exerciseId ON interval_program_exercises(exerciseId)")

                // IntervalRecord テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS interval_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        programName TEXT NOT NULL,
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        workSeconds INTEGER NOT NULL,
                        restSeconds INTEGER NOT NULL,
                        rounds INTEGER NOT NULL,
                        roundRestSeconds INTEGER NOT NULL,
                        completedRounds INTEGER NOT NULL,
                        completedExercisesInLastRound INTEGER NOT NULL,
                        exercisesJson TEXT NOT NULL,
                        comment TEXT
                    )
                """)
            }
        }
        // マイグレーション 18 → 19: TodoTaskにtype/referenceIdカラム追加（プログラム・インターバル対応）
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 新テーブル作成
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS todo_tasks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL DEFAULT 'EXERCISE',
                        referenceId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                """)

                // データ移行（既存はすべてEXERCISE型）
                database.execSQL("""
                    INSERT INTO todo_tasks_new (id, type, referenceId, sortOrder)
                    SELECT id, 'EXERCISE', exerciseId, sortOrder FROM todo_tasks
                """)

                // 入れ替え
                database.execSQL("DROP TABLE todo_tasks")
                database.execSQL("ALTER TABLE todo_tasks_new RENAME TO todo_tasks")
            }
        }
        // マイグレーション 19 → 20: TodoTaskに曜日リピート機能追加
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE todo_tasks ADD COLUMN repeatDays TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE todo_tasks ADD COLUMN lastCompletedDate TEXT"
                )
            }
        }

        // マイグレーション 20 → 21: exercise_groups に displayOrder を追加（グループ並び替え機能用）
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE exercise_groups ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0"
                )
                // 既存グループに名前順で連番を割り当て
                database.execSQL("""
                    UPDATE exercise_groups
                    SET displayOrder = (
                        SELECT COUNT(*)
                        FROM exercise_groups e2
                        WHERE e2.name < exercise_groups.name
                    )
                """)
            }
        }

        // マイグレーション 21 → 22: v2 ワークアウト履歴テーブルを追加（既存テーブルは変更しない、ADR 0002/0003）
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                MIGRATION_21_22_SQL.forEach(database::execSQL)
            }
        }

        // Copied from the Room-generated schema 22 (app/schemas/.../22.json).
        private val MIGRATION_21_22_SQL = listOf(
            "CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `status` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `sourceTemplateId` INTEGER, `sourceNameSnapshot` TEXT, `startedAtEpochMillis` INTEGER NOT NULL, `endedAtEpochMillis` INTEGER, `updatedAtEpochMillis` INTEGER NOT NULL, `timePrecision` TEXT NOT NULL, `comment` TEXT)",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_startedAtEpochMillis` ON `workout_sessions` (`startedAtEpochMillis`)",
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_status` ON `workout_sessions` (`status`)",
            "CREATE TABLE IF NOT EXISTS `session_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutSessionId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `exerciseId` INTEGER, `groupId` INTEGER, `sourceProgramExerciseId` INTEGER, `exerciseNameSnapshot` TEXT NOT NULL, `exerciseKindSnapshot` TEXT NOT NULL, `lateralitySnapshot` TEXT NOT NULL, `groupNameSnapshot` TEXT, `targetSets` INTEGER, `targetRepetitions` INTEGER, `targetDurationMillis` INTEGER, FOREIGN KEY(`workoutSessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`groupId`) REFERENCES `exercise_groups`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_session_exercises_workoutSessionId_orderIndex` ON `session_exercises` (`workoutSessionId`, `orderIndex`)",
            "CREATE INDEX IF NOT EXISTS `index_session_exercises_exerciseId` ON `session_exercises` (`exerciseId`)",
            "CREATE INDEX IF NOT EXISTS `index_session_exercises_groupId` ON `session_exercises` (`groupId`)",
            "CREATE TABLE IF NOT EXISTS `set_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionExerciseId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `setNumber` INTEGER NOT NULL, `roundNumber` INTEGER, `status` TEXT NOT NULL, `side` TEXT NOT NULL, `repetitions` INTEGER, `durationMillis` INTEGER, `distanceCm` INTEGER, `addedWeightGrams` INTEGER, `assistanceGrams` INTEGER, `targetRepetitions` INTEGER, `targetDurationMillis` INTEGER, `startedAtEpochMillis` INTEGER, `completedAtEpochMillis` INTEGER, `timePrecision` TEXT NOT NULL, `comment` TEXT, `legacyTrainingRecordId` INTEGER, FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_set_entries_sessionExerciseId_orderIndex` ON `set_entries` (`sessionExerciseId`, `orderIndex`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_set_entries_legacyTrainingRecordId` ON `set_entries` (`legacyTrainingRecordId`)",
        )

        // マイグレーション 22 → 23: set_entries の legacyTrainingRecordId 一意制約を side 込みに変更
        // （片側種目の旧レコード1件は左右2件の SetEntry になるため）
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP INDEX IF EXISTS `index_set_entries_legacyTrainingRecordId`")
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_set_entries_legacyTrainingRecordId_side` " +
                        "ON `set_entries` (`legacyTrainingRecordId`, `side`)"
                )
            }
        }

        // Version 24: the interval settings a v2 interval workout ran with (nullable, other sources leave them empty).
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                for (column in listOf("intervalWorkSeconds", "intervalRestSeconds", "intervalRounds", "intervalRoundRestSeconds")) {
                    database.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `$column` INTEGER")
                }
            }
        }

        // Oldest installed database version that can migrate to the current version.
        // See docs/development/supported-database-versions.md.
        const val OLDEST_SUPPORTED_VERSION = 9

        // Every registered migration, in order. Declared after the migrations so they
        // are initialized first. Production and migration tests both use this array.
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22,
            MIGRATION_22_23,
            MIGRATION_23_24,
        )
    }
}