package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

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

/** One data row of the records CSV, checked and converted. */
internal data class RecordCsvRow(
    val exerciseName: String,
    val exerciseType: String,
    val date: String,
    val time: String,
    val setNumber: Int,
    val valueRight: Int,
    val valueLeft: Int?,
    val comment: String,
    val distanceCm: Int?,
    val weightG: Int?,
    val assistanceG: Int?,
)

private val CSV_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)
private val CSV_TIME = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT)

/**
 * Reads one data row of the records CSV, or says why it cannot be imported. Dates and times must be
 * in the form the app writes (2026-10-08, 07:30), so imported sets group, sort and edit like recorded
 * ones; numbers must be whole and not negative. An optional number that does not read is an error
 * rather than an empty value, so nothing typed is silently dropped.
 */
internal fun parseRecordCsvRow(columns: List<String>): Result<RecordCsvRow> {
    fun invalid(reason: String) = Result.failure<RecordCsvRow>(IllegalArgumentException(reason))
    if (columns.size < 8) return invalid("Invalid format (not enough columns)")
    val field = { index: Int -> columns.getOrNull(index)?.trim().orEmpty() }
    val name = field(0)
    val type = field(1)
    val date = field(2)
    val time = field(3)
    val setNumber = field(4)
    val valueRight = field(5)
    if (name.isEmpty() || type.isEmpty() || date.isEmpty() || time.isEmpty() || setNumber.isEmpty() || valueRight.isEmpty()) {
        return invalid("Missing required fields")
    }
    if (runCatching { LocalDate.parse(date, CSV_DATE) }.isFailure) return invalid("Invalid date (expected YYYY-MM-DD): $date")
    if (runCatching { LocalTime.parse(time, CSV_TIME) }.isFailure) return invalid("Invalid time (expected HH:MM): $time")
    val optional = listOf(6, 8, 9, 10).associateWith { field(it) }
    val numbers = (listOf(setNumber, valueRight) + optional.values.filter { it.isNotEmpty() }).map { it.toIntOrNull() }
    if (numbers.any { it == null }) return invalid("Invalid number format")
    if (numbers.any { it!! < 0 }) return invalid("Negative value")
    fun number(index: Int) = optional.getValue(index).takeIf { it.isNotEmpty() }?.toInt()
    return Result.success(
        RecordCsvRow(
            exerciseName = name,
            exerciseType = type,
            date = date,
            time = time,
            setNumber = setNumber.toInt(),
            valueRight = valueRight.toInt(),
            valueLeft = number(6),
            comment = field(7),
            distanceCm = number(8),
            weightG = number(9),
            assistanceG = number(10),
        )
    )
}
