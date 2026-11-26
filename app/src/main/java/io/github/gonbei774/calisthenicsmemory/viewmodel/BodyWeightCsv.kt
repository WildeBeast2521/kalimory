package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime

/** One weight read from a file: the day it counts from and its weight in grams. */
internal class ParsedWeight(val date: LocalDate, val weightG: Int, val line: Int)

/**
 * What a body weight CSV holds: one reading per date, the rows that were not used, and the lines that
 * could not be read. [unusable] is set when the file has no date or no weight column.
 */
internal class BodyWeightFile(
    val readings: List<ParsedWeight>,
    /** Rows for a date the file already has a later reading for. */
    val sameDateRows: Int,
    /** Rows with no weight, as openScale writes for a reading without one. */
    val emptyWeightRows: Int,
    val errors: List<ImportIssue>,
    val unusable: ImportIssue?,
)

/**
 * Reads this app's own body weight export (`date,weightKg`) and openScale's export (a `DATE`, a `TIME`
 * and a `WEIGHT` among many columns): columns are found by header name, whatever else the file holds
 * (ADR 0016). Weights are read as kilograms. Dates must be ISO ("2026-11-11"): a form such as 11/10
 * is never guessed. Several readings on one date keep the latest time, or the last one in the file.
 */
internal object BodyWeightCsv {
    private const val BOM = "\uFEFF"

    private fun separatorOf(headerLine: String) = if (';' in headerLine && ',' !in headerLine) ';' else ','

    private fun headerNames(headerLine: String) = parseCsv(headerLine, separatorOf(headerLine)).firstOrNull().orEmpty().map { it.trim().lowercase() }

    /** True when the first line names a date and a weight column. */
    fun looksLikeBodyWeights(headerLine: String): Boolean {
        val names = headerNames(headerLine.removePrefix(BOM))
        return "date" in names && ("weightkg" in names || "weight" in names)
    }

    fun parse(text: String): BodyWeightFile {
        val clean = text.removePrefix(BOM)
        val first = clean.lineSequence().firstOrNull { it.isNotBlank() && !it.startsWith("#") } ?: ""
        val separator = separatorOf(first)
        val records = parseCsv(clean, separator).filter { row -> row.any { it.isNotBlank() } && !row.first().startsWith("#") }
        val header = records.firstOrNull()?.map { it.trim().lowercase() }.orEmpty()
        val dateColumn = header.indexOf("date")
        val weightColumn = header.indexOf("weightkg").takeIf { it >= 0 } ?: header.indexOf("weight")
        val timeColumn = header.indexOf("time")
        if (dateColumn < 0 || weightColumn < 0) {
            return BodyWeightFile(emptyList(), 0, 0, emptyList(), ImportIssue(R.string.body_weight_issue_columns))
        }

        class Reading(val weight: ParsedWeight, val time: LocalTime?)
        val byDate = linkedMapOf<LocalDate, Reading>()
        val errors = mutableListOf<ImportIssue>()
        var sameDate = 0
        var empty = 0
        records.drop(1).forEachIndexed { index, row ->
            val line = index + 2
            val dateText = row.getOrNull(dateColumn)?.trim().orEmpty()
            val weightText = row.getOrNull(weightColumn)?.trim().orEmpty()
            if (weightText.isEmpty()) { empty++; return@forEachIndexed }
            val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
            if (date == null) { errors += ImportIssue(R.string.body_weight_issue_date, listOf(dateText)).atLine(line); return@forEachIndexed }
            val kg = (if (separator == ';') weightText.replace(',', '.') else weightText).toBigDecimalOrNull()
            if (kg == null) { errors += ImportIssue(R.string.body_weight_issue_weight, listOf(weightText)).atLine(line); return@forEachIndexed }
            val grams = kg.multiply(BigDecimal(1000)).setScale(0, RoundingMode.HALF_UP)
            if (grams < BigDecimal(1_000) || grams > BigDecimal(500_000)) {
                errors += ImportIssue(R.string.body_weight_issue_range, listOf(weightText)).atLine(line)
                return@forEachIndexed
            }
            val time = timeColumn.takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            val reading = Reading(ParsedWeight(date, grams.toInt(), line), time)
            val earlier = byDate[date]
            if (earlier == null) byDate[date] = reading
            else {
                sameDate++
                // The later time wins; with no times to compare, the later line.
                if (time == null || earlier.time == null || time >= earlier.time) byDate[date] = reading
            }
        }
        return BodyWeightFile(byDate.values.map { it.weight }.sortedBy { it.date }, sameDate, empty, errors, null)
    }
}
