package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R

private fun onest(weight: Int) = Font(
    R.font.onest_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/**
 * Onest (SIL Open Font License), bundled. It covers Latin and Cyrillic; Arabic, Japanese and
 * Chinese text falls back to the system font.
 */
val Onest = FontFamily(onest(400), onest(500), onest(600), onest(700), onest(800))

private fun style(size: Int, line: Int, weight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

/** The Material type scale, set in Onest with calm weights: bold only where it leads. */
val Typography = Typography(
    displayLarge = style(56, 64, 700, -0.02),
    displayMedium = style(44, 52, 700, -0.02),
    displaySmall = style(36, 44, 700, -0.01),
    headlineLarge = style(32, 40, 650, -0.01),
    headlineMedium = style(28, 36, 650),
    headlineSmall = style(24, 32, 600),
    titleLarge = style(22, 28, 600),
    titleMedium = style(16, 24, 600, 0.005),
    titleSmall = style(14, 20, 600, 0.005),
    bodyLarge = style(16, 24, 400),
    bodyMedium = style(14, 20, 400),
    bodySmall = style(12, 16, 400, 0.01),
    labelLarge = style(14, 20, 500, 0.01),
    labelMedium = style(12, 16, 500, 0.02),
    labelSmall = style(11, 16, 500, 0.02),
)

/**
 * The one bold element (ADR 0004, decision 5): workout counts and times, heavy and with
 * tabular figures so the digits do not shift as they change.
 */
val WorkoutNumerals = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight(800),
    fontSize = 96.sp,
    lineHeight = 104.sp,
    letterSpacing = (-0.03).em,
    fontFeatureSettings = "tnum",
)
