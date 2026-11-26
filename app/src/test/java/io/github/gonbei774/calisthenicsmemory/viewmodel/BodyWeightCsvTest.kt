package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.screens.detectCsvType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Reading a body weight CSV: this app's own export and openScale's (ADR 0016). */
class BodyWeightCsvTest {
    // An openScale export written as it writes it: a byte order mark, CRLF, many empty columns, a time with milliseconds.
    private val openScale = "\uFEFF" +
        "DATE,TIME,BCM,BICEPS,BMI,BMR,BODY_FAT,BONE,CALIPER,CALIPER_1,CALIPER_2,CALIPER_3,CALORIES,CHEST,COMMENT,ECW,HEART_RATE,HIPS,ICW,IMPEDANCE,IMPEDANCE_LOW,LBM,MUSCLE,NECK,PROTEIN,TDEE,THIGH,VISCERAL_FAT,WAIST,WATER,WEIGHT,WHR,WHTR\r\n" +
        "2026-11-11,08:31:43,,,22.8,1501.0,15.4,2.66,,,,,1750.0,,,,,,,439.0,,53.17,42.6,,,2063.87,,,,57.59,62.85,,\r\n" +
        "2026-08-08,16:27:21.835,,,24.64,1551.5,,,,,,,1700.0,,,,,,,,,,,,22.0,2133.31,,,,,64.9,,\r\n"

    @Test fun `openScale's export is read by its date and weight columns, whatever else it holds`() {
        val file = BodyWeightCsv.parse(openScale)
        assertNull(file.unusable)
        assertEquals(listOf(LocalDate.of(2026, 8, 8) to 64_900, LocalDate.of(2026, 11, 11) to 62_850), file.readings.map { it.date to it.weightG })
        assertTrue(file.errors.isEmpty())
    }

    @Test fun `this app's own export reads back`() {
        val file = BodyWeightCsv.parse("date,weightKg\n2026-10-12,65\n2026-11-11,62.85\n")
        assertEquals(listOf(65_000, 62_850), file.readings.map { it.weightG })
    }

    @Test fun `a semicolon file with decimal commas is read`() {
        val file = BodyWeightCsv.parse("DATE;TIME;WEIGHT\n2026-11-11;08:00:00;62,85\n")
        assertEquals(62_850, file.readings.single().weightG)
    }

    @Test fun `a file with no date or no weight column is refused whole`() {
        assertEquals(R.string.body_weight_issue_columns, BodyWeightCsv.parse("when,kilos\n2026-11-11,60\n").unusable?.res)
        assertEquals(R.string.body_weight_issue_columns, BodyWeightCsv.parse("date,bmi\n2026-11-11,22\n").unusable?.res)
        assertNotNull(BodyWeightCsv.parse("").unusable)
    }

    @Test fun `a bad line is reported with its line number and the others are still read`() {
        val file = BodyWeightCsv.parse("date,weightKg\n11/10/2026,60\n2026-11-11,sixty\n2026-11-12,0\n2026-11-13,501\n2026-11-14,61\n")
        assertEquals(listOf(R.string.body_weight_issue_date, R.string.body_weight_issue_weight, R.string.body_weight_issue_range, R.string.body_weight_issue_range), file.errors.map { it.res })
        assertEquals(listOf(2, 3, 4, 5), file.errors.map { it.args.first() })
        assertEquals(listOf(61_000), file.readings.map { it.weightG })
    }

    @Test fun `an empty weight is skipped, not an error`() {
        val file = BodyWeightCsv.parse("date,weightKg\n2026-11-11,\n2026-11-12,60\n")
        assertEquals(1, file.emptyWeightRows)
        assertTrue(file.errors.isEmpty())
        assertEquals(1, file.readings.size)
    }

    @Test fun `several readings on one date keep the latest time, or the last line`() {
        val byTime = BodyWeightCsv.parse("DATE,TIME,WEIGHT\n2026-11-11,20:00:00,63\n2026-11-11,07:00:00,62\n2026-11-12,08:00:00,61\n")
        assertEquals(listOf(63_000, 61_000), byTime.readings.map { it.weightG })
        assertEquals(1, byTime.sameDateRows)
        val noTime = BodyWeightCsv.parse("date,weightKg\n2026-11-11,62\n2026-11-11,63\n")
        assertEquals(63_000, noTime.readings.single().weightG)
    }

    @Test fun `a weight is rounded to the gram, and quoted fields are read`() {
        assertEquals(62_851, BodyWeightCsv.parse("date,weightKg\n2026-11-11,\"62.8505\"\n").readings.single().weightG)
    }

    @Test fun `a comment line and blank lines are ignored`() {
        val file = BodyWeightCsv.parse("# exported\n\ndate,weightKg\n\n2026-11-11,60\n")
        assertEquals(1, file.readings.size)
    }

    @Test fun `a body weight file is told apart from the other CSV files, with or without a byte order mark`() {
        assertEquals(CsvType.BODY_WEIGHTS, detectCsvType(openScale))
        assertEquals(CsvType.BODY_WEIGHTS, detectCsvType("date,weightKg\n2026-11-11,60\n"))
        assertEquals(CsvType.GROUPS, detectCsvType("name\nPush\n"))
        assertEquals(CsvType.EXERCISES, detectCsvType("\uFEFFname,type,group,sortOrder,laterality\n"))
        assertEquals(CsvType.RECORDS, detectCsvType("exerciseName,exerciseType,date\n"))
        assertNull(detectCsvType("hello,world\n1,2\n"))
    }
}
