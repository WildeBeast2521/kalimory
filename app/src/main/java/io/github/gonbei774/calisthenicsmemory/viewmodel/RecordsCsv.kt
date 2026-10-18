package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet

/** The records CSV header (format v12, 11 columns). */
internal const val RECORDS_CSV_HEADER =
    "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment,distanceCm,weightG,assistanceG"

/**
 * Every set in [records], legacy or v2, as the records CSV: sorted by date, time and set number,
 * one row per set. Sets of an exercise no longer in [exercises] are left out, as they have no name.
 */
internal fun recordsCsv(records: List<HistorySet>, exercises: List<Exercise>): String {
    val exerciseMap = exercises.associateBy { it.id }
    val csv = StringBuilder()
    csv.appendLine(RECORDS_CSV_HEADER)
    records
        .sortedWith(compareBy({ it.date }, { it.time }, { it.setNumber }))
        .forEach { record ->
            val exercise = exerciseMap[record.exerciseId] ?: return@forEach
            csv.appendLine(
                csvRow(
                    exercise.name,
                    exercise.type,
                    record.date,
                    record.time,
                    record.setNumber.toString(),
                    record.valueRight.toString(),
                    record.valueLeft?.toString() ?: "",
                    record.comment,
                    record.distanceCm?.toString() ?: "",
                    record.weightG?.toString() ?: "",
                    record.assistanceG?.toString() ?: "",
                )
            )
        }
    return csv.toString()
}
