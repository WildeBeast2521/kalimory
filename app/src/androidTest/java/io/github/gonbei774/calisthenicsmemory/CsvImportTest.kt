package io.github.gonbei774.calisthenicsmemory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.*
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import io.github.gonbei774.calisthenicsmemory.viewmodel.CsvImportReport
import io.github.gonbei774.calisthenicsmemory.viewmodel.CsvTransfer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/** The app's CSV merge import ([CsvTransfer]) on an in-memory Room database. */
@RunWith(AndroidJUnit4::class)
class CsvImportTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var groupDao: ExerciseGroupDao
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        groupDao = database.exerciseGroupDao()
        exerciseDao = database.exerciseDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun importGroups_merge_successfulImport() = runBlocking {
        val csv = """
            name
            Step 1 Pushups
            Step 2 Squats
        """.trimIndent()

        val report = importGroupsCsv(csv)

        assertEquals(2, report.successCount)
        assertEquals(0, report.skippedCount)
        assertEquals(0, report.errorCount)

        val groups = groupDao.getAllGroups()
        // Note: Flow is not collected in test, so we use suspend function instead
        val group1 = groupDao.getGroupByName("Step 1 Pushups")
        val group2 = groupDao.getGroupByName("Step 2 Squats")

        assertNotNull(group1)
        assertNotNull(group2)
        assertEquals("Step 1 Pushups", group1?.name)
        assertEquals("Step 2 Squats", group2?.name)
    }

    @Test
    fun importGroups_merge_skipDuplicates() = runBlocking {
        // Insert existing group
        groupDao.insertGroup(ExerciseGroup(name = "Step 1 Pushups"))

        val csv = """
            name
            Step 1 Pushups
            Step 2 Squats
        """.trimIndent()

        val report = importGroupsCsv(csv)

        assertEquals(1, report.successCount)
        assertEquals(1, report.skippedCount)
        assertEquals(0, report.errorCount)
        assertEquals(1, report.skippedItems.size)
        assertTrue(report.skippedItems[0].args.toString().contains("Step 1 Pushups"))
    }

    @Test
    fun importGroups_emptyName_error() = runBlocking {
        val csv = """
            name
            Step 1 Pushups

            Step 2 Squats
        """.trimIndent()

        val report = importGroupsCsv(csv)

        assertEquals(2, report.successCount)
        assertEquals(0, report.skippedCount)
        // Empty line is filtered out, so no error
        assertEquals(0, report.errorCount)
    }

    @Test
    fun importExercises_merge_successfulImport() = runBlocking {
        // Insert required group
        groupDao.insertGroup(ExerciseGroup(name = "Step 1 Pushups"))

        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(1, report.successCount)
        assertEquals(0, report.skippedCount)
        assertEquals(0, report.errorCount)

        val exercise = exerciseDao.getExerciseByNameAndType("Wall Push-up", "Dynamic")
        assertNotNull(exercise)
        assertEquals("Wall Push-up", exercise?.name)
        assertEquals("Dynamic", exercise?.type)
        assertEquals("Step 1 Pushups", exercise?.group)
        assertEquals(1, exercise?.sortOrder)
        assertEquals("Bilateral", exercise?.laterality)
        assertEquals(3, exercise?.targetSets)
        assertEquals(50, exercise?.targetValue)
        assertEquals(false, exercise?.isFavorite)
    }

    @Test
    fun importRecords_writesWorkoutsInTheCurrentModelAndNoOlderRecords() = runBlocking {
        seedExercises()
        val csv = listOf(
            recordsHeader,
            "Push-up,Dynamic,2026-03-09,07:30,1,10,,morning,,,",
            "Push-up,Dynamic,2026-03-09,07:30,2,9,,morning,,,",
            "Lunge,Dynamic,2026-03-09,07:30,1,8,6,morning,,,",
        ).joinToString("\n")

        val report = transfer().importRecordsFromCsv(csv)

        assertEquals(3, report.successCount)
        assertEquals(0, report.errorCount)
        assertEquals(0, database.trainingRecordDao().countByExercise(exerciseDao.getExerciseByNameAndType("Push-up", "Dynamic")!!.id))
        val sessions = database.backupDao().snapshot().workoutSessions
        assertEquals(1, sessions.size)
        assertEquals("morning", sessions.single().comment)
    }

    @Test
    fun importRecords_historyShowsEachImportedSetOnce() = runBlocking {
        seedExercises()
        val csv = listOf(
            recordsHeader,
            "Push-up,Dynamic,2026-03-09,07:30,1,10,,,,,",
            "Lunge,Dynamic,2026-03-09,07:30,1,8,6,,,,",
        ).joinToString("\n")
        transfer().importRecordsFromCsv(csv)

        val shown = history()
        assertEquals(2, shown.size)
        assertEquals(listOf(8 to 6), shown.filter { it.valueLeft != null }.map { it.valueRight to it.valueLeft!! })
    }

    @Test
    fun importRecords_theSameFileTwiceAddsNothing() = runBlocking {
        seedExercises()
        val csv = listOf(
            recordsHeader,
            "Push-up,Dynamic,2026-03-09,07:30,1,10,,,,,",
            "Push-up,Dynamic,2026-03-09,07:30,2,9,,,,,",
        ).joinToString("\n")
        transfer().importRecordsFromCsv(csv)
        val second = transfer().importRecordsFromCsv(csv)

        assertEquals(0, second.successCount)
        assertEquals(2, second.skippedCount)
        assertEquals(2, history().size)
    }

    @Test
    fun importRecords_anExportedFileImportedBackAddsNothing() = runBlocking {
        seedExercises()
        transfer().importRecordsFromCsv(listOf(recordsHeader, "Push-up,Dynamic,2026-03-09,07:30,1,10,,,,,", "Push-up,Dynamic,2026-03-09,07:30,2,9,,,,,").joinToString("\n"))
        val exported = transfer().exportRecords()
        val report = transfer().importRecordsFromCsv(exported)

        assertEquals(0, report.successCount)
        assertEquals(2, history().size)
    }

    @Test
    fun importRecords_warmUpAndEffortAreKeptAndExported() = runBlocking {
        seedExercises()
        val csv = listOf(
            recordsHeader + ",warmUp,effortLeft",
            "Push-up,Dynamic,2026-03-09,07:30,1,12,,,,,,true,4",
            "Push-up,Dynamic,2026-03-09,07:30,2,10,,,,,,false,2",
        ).joinToString("\n")
        val report = transfer().importRecordsFromCsv(csv)

        assertEquals(2, report.successCount)
        val shown = history().sortedBy { it.setNumber }
        assertEquals(listOf(true, false), shown.map { it.warmUp })
        assertEquals(listOf(4, 2), shown.map { it.effortLeft })
        val exported = transfer().exportRecords()
        assertTrue(exported.contains(",true,4"))
        assertTrue(exported.contains(",false,2"))
    }

    @Test
    fun importRecords_marksOnASetBesideExistingSetsAreRefusedNotStripped() = runBlocking {
        seedExercises()
        transfer().importRecordsFromCsv(listOf(recordsHeader, "Push-up,Dynamic,2026-03-09,07:30,1,10,,,,,").joinToString("\n"))
        val report = transfer().importRecordsFromCsv(
            listOf(recordsHeader + ",warmUp,effortLeft", "Push-up,Dynamic,2026-03-09,07:30,2,9,,,,,,true,").joinToString("\n")
        )
        assertEquals(0, report.successCount)
        assertEquals(1, report.errorCount)
        assertEquals(1, history().size)
    }

    @Test
    fun importRecords_badMarkValueIsALineError() = runBlocking {
        seedExercises()
        val report = transfer().importRecordsFromCsv(
            listOf(recordsHeader + ",warmUp,effortLeft", "Push-up,Dynamic,2026-03-09,07:30,1,10,,,,,,maybe,").joinToString("\n")
        )
        assertEquals(1, report.errorCount)
        assertEquals(0, history().size)
    }

    @Test
    fun importExercises_catalogId_linked() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId
            Wall push-up,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,push.wall
        """.trimIndent()
        val report = importExercisesCsv(csv)
        assertEquals(1, report.successCount)
        assertEquals("push.wall", exerciseDao.getExerciseByNameAndType("Wall push-up", "Dynamic")?.catalogId)
    }

    @Test
    fun importExercises_catalogIdAlreadyLinked_addedWithoutLink() = runBlocking {
        val first = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId
            Wall push-up,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,push.wall
        """.trimIndent()
        importExercisesCsv(first)
        val second = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId
            My wall push-up,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,push.wall
        """.trimIndent()
        val report = importExercisesCsv(second)
        assertEquals(1, report.successCount)
        assertEquals(0, report.skippedCount)
        assertEquals(1, report.skippedItems.size)
        assertEquals(null, exerciseDao.getExerciseByNameAndType("My wall push-up", "Dynamic")?.catalogId)
        assertEquals("push.wall", exerciseDao.getExerciseByNameAndType("Wall push-up", "Dynamic")?.catalogId)
    }

    @Test
    fun importExercises_muscles_chosenForAnOwnExercise() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId,primaryMuscles,secondaryMuscles
            Biceps curl,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,,biceps,forearms;lower back
        """.trimIndent()
        val report = importExercisesCsv(csv)
        assertEquals(1, report.successCount)
        val curl = exerciseDao.getExerciseByNameAndType("Biceps curl", "Dynamic")!!
        assertEquals(setOf(io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle.BICEPS), curl.primaryMuscles)
        assertEquals(
            setOf(io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle.FOREARMS, io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle.LOWER_BACK),
            curl.secondaryMuscles,
        )
    }

    @Test
    fun importExercises_oldFileWithoutMuscles_hasNone() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId
            Plain curl,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,
        """.trimIndent()
        assertEquals(1, importExercisesCsv(csv).successCount)
        val curl = exerciseDao.getExerciseByNameAndType("Plain curl", "Dynamic")!!
        assertTrue(curl.primaryMuscles.isEmpty() && curl.secondaryMuscles.isEmpty())
    }

    @Test
    fun importExercises_unknownMuscle_error() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId,primaryMuscles,secondaryMuscles
            Biceps curl,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,,biceps;wings,
        """.trimIndent()
        val report = importExercisesCsv(csv)
        assertEquals(0, report.successCount)
        assertEquals(1, report.errorCount)
        assertEquals(null, exerciseDao.getExerciseByNameAndType("Biceps curl", "Dynamic"))
    }

    @Test
    fun importExercises_bodyWeightPercent_read() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId,primaryMuscles,secondaryMuscles,bodyWeightPercent
            Push-up,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,,,,65
        """.trimIndent()
        assertEquals(1, importExercisesCsv(csv).successCount)
        assertEquals(65, exerciseDao.getExerciseByNameAndType("Push-up", "Dynamic")?.bodyWeightPercent)
    }

    @Test
    fun importExercises_bodyWeightPercent_outOfRangeOrNotANumber_error() = runBlocking {
        listOf("0", "101", "abc", "-5").forEachIndexed { i, bad ->
            val csv = """
                name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId,primaryMuscles,secondaryMuscles,bodyWeightPercent
                Bad $i,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,,,,$bad
            """.trimIndent()
            val report = importExercisesCsv(csv)
            assertEquals("share $bad", 0, report.successCount)
            assertEquals("share $bad", 1, report.errorCount)
            assertEquals(null, exerciseDao.getExerciseByNameAndType("Bad $i", "Dynamic"))
        }
    }

    @Test
    fun importExercises_oldFileWithoutTheShare_hasNone() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId
            Old push-up,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,
        """.trimIndent()
        assertEquals(1, importExercisesCsv(csv).successCount)
        assertEquals(null, exerciseDao.getExerciseByNameAndType("Old push-up", "Dynamic")?.bodyWeightPercent)
    }

    @Test
    fun exportedExercises_carryTheShare_andComeBackWithIt() = runBlocking {
        exerciseDao.insertExercise(io.github.gonbei774.calisthenicsmemory.data.Exercise(name = "Shared push-up", type = "Dynamic", bodyWeightPercent = 65))
        exerciseDao.insertExercise(io.github.gonbei774.calisthenicsmemory.data.Exercise(name = "Plain squat", type = "Dynamic"))
        val csv = transfer().exportExercises()
        assertTrue(csv.lines().first().endsWith("bodyWeightPercent"))
        database.clearAllTables()
        assertEquals(2, importExercisesCsv(csv).successCount)
        assertEquals(65, exerciseDao.getExerciseByNameAndType("Shared push-up", "Dynamic")?.bodyWeightPercent)
        assertEquals(null, exerciseDao.getExerciseByNameAndType("Plain squat", "Dynamic")?.bodyWeightPercent)
    }

    @Test
    fun exportedBodyWeights_areDateAndExactKilograms() = runBlocking {
        database.bodyWeightDao().insert(io.github.gonbei774.calisthenicsmemory.data.BodyWeight(date = "2026-11-11", weightG = 62_850))
        database.bodyWeightDao().insert(io.github.gonbei774.calisthenicsmemory.data.BodyWeight(date = "2026-10-12", weightG = 65_000))
        assertEquals("date,weightKg\n2026-10-12,65\n2026-11-11,62.85\n", transfer().exportBodyWeights().replace("\r\n", "\n"))
    }

    @Test
    fun importBodyWeights_addsNewDatesOnly_andUndoRemovesOnlyThoseEntries() = runBlocking {
        val dao = database.bodyWeightDao()
        // One entry typed by hand, on a date the file also has: it must stay as it is, and survive an undo.
        dao.insert(io.github.gonbei774.calisthenicsmemory.data.BodyWeight(date = "2026-11-11", weightG = 70_000))
        val csv = "\uFEFFDATE,TIME,WEIGHT\r\n2026-11-11,08:31:43,62.85\r\n2026-08-08,16:27:21.835,64.9\r\n2026-09-09,,x\r\n"

        val preview = transfer().previewBodyWeights(csv)
        assertEquals(1, preview.toAdd); assertEquals(1, preview.alreadyThere); assertEquals(1, preview.unreadable)
        assertEquals(1, dao.latestImportId().let { if (it == null) 1 else 0 }) // the preview wrote nothing

        val report = transfer().importBodyWeights(csv)
        assertEquals(1, report.successCount)
        assertEquals(1, report.skippedItems.size)
        assertEquals(1, report.errorCount)
        val after = database.backupDao().bodyWeights()
        assertEquals(listOf("2026-08-08" to 64_900, "2026-11-11" to 70_000), after.map { it.date to it.weightG })
        val importId = dao.latestImportId()!!
        assertEquals(1, dao.countImport(importId))

        // An edited import is the user's now, and an undo leaves it alone.
        assertEquals(1, dao.deleteImport(importId))
        assertEquals(listOf("2026-11-11"), database.backupDao().bodyWeights().map { it.date })
    }

    @Test
    fun importBodyWeights_aFileWithoutAWeightColumnWritesNothing() = runBlocking {
        val report = transfer().importBodyWeights("date,bmi\n2026-11-11,22\n")
        assertEquals(0, report.successCount)
        assertEquals(1, report.errorCount)
        assertEquals(0, database.backupDao().bodyWeights().size)
    }

    @Test
    fun importExercises_malformedCatalogId_error() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId
            Wall push-up,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,Not A Step
        """.trimIndent()
        val report = importExercisesCsv(csv)
        assertEquals(0, report.successCount)
        assertEquals(1, report.errorCount)
        assertEquals(null, exerciseDao.getExerciseByNameAndType("Wall push-up", "Dynamic"))
    }

    @Test
    fun importExercises_existingExerciseLinkNotChanged() = runBlocking {
        val first = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId
            Wall push-up,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,push.wall
        """.trimIndent()
        importExercisesCsv(first)
        val again = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description,catalogId
            Wall push-up,Dynamic,,0,Bilateral,3,10,false,0,,,false,false,false,,push.incline
        """.trimIndent()
        val report = importExercisesCsv(again)
        assertEquals(0, report.successCount)
        assertEquals(1, report.skippedCount)
        assertEquals("push.wall", exerciseDao.getExerciseByNameAndType("Wall push-up", "Dynamic")?.catalogId)
    }

    @Test
    fun importExercises_oldFileWithoutCatalogId_readsAsBefore() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Plain squat,Dynamic,,0,Bilateral,3,15,false
        """.trimIndent()
        val report = importExercisesCsv(csv)
        assertEquals(1, report.successCount)
        assertEquals(null, exerciseDao.getExerciseByNameAndType("Plain squat", "Dynamic")?.catalogId)
    }

    @Test
    fun importExercises_invalidType_error() = runBlocking {
        groupDao.insertGroup(ExerciseGroup(name = "Step 1 Pushups"))

        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,InvalidType,Step 1 Pushups,1,Bilateral,3,50,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(0, report.successCount)
        assertEquals(0, report.skippedCount)
        assertEquals(1, report.errorCount)
        assertEquals(R.string.csv_issue_type, report.errors[0].res)
    }

    @Test
    fun importExercises_invalidLaterality_error() = runBlocking {
        groupDao.insertGroup(ExerciseGroup(name = "Step 1 Pushups"))

        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,InvalidLaterality,3,50,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(0, report.successCount)
        assertEquals(0, report.skippedCount)
        assertEquals(1, report.errorCount)
        assertEquals(R.string.csv_issue_laterality, report.errors[0].res)
    }

    @Test
    fun importExercises_unknownGroup_isCreated() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,New Group,1,Bilateral,3,50,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(1, report.successCount)
        assertEquals(0, report.errorCount)
        assertEquals(1, report.groupsCreatedCount)
        assertNotNull(groupDao.getGroupByName("New Group"))
        assertEquals("New Group", exerciseDao.getExerciseByNameAndType("Wall Push-up", "Dynamic")?.group)
    }

    @Test
    fun importExercises_repeatedInTheFile_isSkippedOnce() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Plank,Isometric,,1,Bilateral,3,60,false
            Plank,Isometric,,1,Bilateral,3,60,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(1, report.successCount)
        assertEquals(1, report.skippedCount)
        assertEquals(0, report.errorCount)
    }

    @Test
    fun importExercises_duplicateDifferentLaterality_skip() = runBlocking {
        groupDao.insertGroup(ExerciseGroup(name = "Step 1 Pushups"))

        // Insert existing exercise with Bilateral
        exerciseDao.insertExercise(
            Exercise(
                name = "Wall Push-up",
                type = "Dynamic",
                group = "Step 1 Pushups",
                sortOrder = 1,
                laterality = "Bilateral",
                targetSets = 3,
                targetValue = 50,
                isFavorite = false
            )
        )

        // Try to import same exercise with Unilateral
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,Unilateral,3,50,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(0, report.successCount)
        assertEquals(1, report.skippedCount)
        assertEquals(0, report.errorCount)
        assertEquals(R.string.csv_skip_mismatch, report.skippedItems[0].res)
    }

    // The app's own import, on the in-memory database.
    private val messages = mutableListOf<UiMessage>()
    private fun transfer() = CsvTransfer(database) { messages += it }

    private val recordsHeader = "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment,distanceCm,weightG,assistanceG"

    private suspend fun seedExercises() {
        exerciseDao.insertExercise(io.github.gonbei774.calisthenicsmemory.data.Exercise(name = "Push-up", type = "Dynamic", laterality = "Bilateral"))
        exerciseDao.insertExercise(io.github.gonbei774.calisthenicsmemory.data.Exercise(name = "Lunge", type = "Dynamic", laterality = "Unilateral"))
    }

    private suspend fun history() = io.github.gonbei774.calisthenicsmemory.data.v2.CompatibilityHistory.read(database, java.time.ZoneId.systemDefault())

    private suspend fun importGroupsCsv(csvString: String): CsvImportReport = transfer().importGroups(csvString)

    private suspend fun importExercisesCsv(csvString: String): CsvImportReport = transfer().importExercises(csvString)
}
