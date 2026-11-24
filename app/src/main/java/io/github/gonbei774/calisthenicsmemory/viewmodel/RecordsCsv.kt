package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.Effort
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * The records CSV header (16 columns). The last five are optional on import, so older files still read: warmUp, effortLeft,
 * and the left side's own weightLeftG, assistanceLeftG and distanceLeftCm, where an empty cell means "the same as the right side".
 */
internal const val RECORDS_CSV_HEADER =
    "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment,distanceCm,weightG,assistanceG,warmUp,effortLeft,weightLeftG,assistanceLeftG,distanceLeftCm"

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
                    record.warmUp.toString(),
                    record.effortLeft?.toString() ?: "",
                    // A left value is written only when the sides differ; a legacy row and an equal value stay empty.
                    leftCell(record.valueLeft, record.weightG, record.weightLeftG),
                    leftCell(record.valueLeft, record.assistanceG, record.assistanceLeftG),
                    leftCell(record.valueLeft, record.distanceCm, record.distanceLeftCm),
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
    val warmUp: Boolean = false,
    val effortLeft: Int? = null,
    /** The left side's own values; null means the same as the right side. */
    val weightLeftG: Int? = null,
    val assistanceLeftG: Int? = null,
    val distanceLeftCm: Int? = null,
)

/** The cell for a left-side value: only when the set has a left side and it differs from the right side's. */
private fun leftCell(valueLeft: Int?, right: Int?, left: Int?): String =
    if (valueLeft != null && left != right) left?.toString() ?: "" else ""

private val CSV_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)
private val CSV_TIME = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT)

/**
 * Reads one data row of the records CSV, or says why it cannot be imported. Dates and times must be
 * in the form the app writes (2026-10-08, 07:30), so imported sets group, sort and edit like recorded
 * ones; numbers must be whole and not negative. An optional number that does not read is an error
 * rather than an empty value, so nothing typed is silently dropped.
 */
internal fun parseRecordCsvRow(columns: List<String>): Result<RecordCsvRow> {
    fun invalid(res: Int, vararg args: Any) = Result.failure<RecordCsvRow>(ImportIssueException(ImportIssue(res, args.toList())))
    if (columns.size < 8) return invalid(R.string.csv_issue_record_columns)
    val field = { index: Int -> columns.getOrNull(index)?.trim().orEmpty() }
    val name = field(0)
    val type = field(1)
    val date = field(2)
    val time = field(3)
    val setNumber = field(4)
    val valueRight = field(5)
    if (name.isEmpty() || type.isEmpty() || date.isEmpty() || time.isEmpty() || setNumber.isEmpty() || valueRight.isEmpty()) {
        return invalid(R.string.csv_issue_record_required)
    }
    if (runCatching { LocalDate.parse(date, CSV_DATE) }.isFailure) return invalid(R.string.csv_issue_date, date)
    if (runCatching { LocalTime.parse(time, CSV_TIME) }.isFailure) return invalid(R.string.csv_issue_time, time)
    val optional = listOf(6, 8, 9, 10, 13, 14, 15).associateWith { field(it) }
    val numbers = (listOf(setNumber, valueRight) + optional.values.filter { it.isNotEmpty() }).map { it.toIntOrNull() }
    if (numbers.any { it == null }) return invalid(R.string.csv_issue_number)
    // Every number but the distance must be 0 or more; a distance may be negative, as every distance
    // input in the app allows, so a file the app wrote always reads back.
    val mustBePositive = listOf(setNumber, valueRight) + listOf(6, 9, 10, 13, 14).map { optional.getValue(it) }.filter { it.isNotEmpty() }
    if (mustBePositive.any { it.toInt() < 0 }) return invalid(R.string.csv_issue_negative)
    fun number(index: Int) = optional.getValue(index).takeIf { it.isNotEmpty() }?.toInt()
    // Warm-up and effort (optional): a value that does not read is an error, never quietly an empty cell.
    val warmUpText = field(11)
    val warmUp = if (warmUpText.isEmpty()) false else warmUpText.toBooleanStrictOrNull() ?: return invalid(R.string.csv_issue_warmup, warmUpText)
    val effortText = field(12)
    val effortLeft = if (effortText.isEmpty()) null else effortText.toIntOrNull()?.takeIf { it in 0..Effort.MAX_EFFORT_LEFT }
        ?: return invalid(R.string.csv_issue_effort, effortText, Effort.MAX_EFFORT_LEFT)
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
            warmUp = warmUp,
            effortLeft = effortLeft,
            weightLeftG = number(13),
            assistanceLeftG = number(14),
            distanceLeftCm = number(15),
        )
    )
}
