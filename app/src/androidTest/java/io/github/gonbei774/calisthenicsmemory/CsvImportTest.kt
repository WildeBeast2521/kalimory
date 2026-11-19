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
        assertTrue(report.skippedItems[0].contains("Step 1 Pushups"))
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
        assertTrue(report.errors[0].contains("type"))
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
        assertTrue(report.errors[0].contains("laterality"))
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
        assertTrue(report.skippedItems[0].contains("laterality"))
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
