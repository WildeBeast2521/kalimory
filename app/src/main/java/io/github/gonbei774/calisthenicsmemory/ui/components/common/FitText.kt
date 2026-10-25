package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

/**
 * A one-line label in a space of fixed width (a tab, a navigation item, a day of the week, a button
 * in a row). With Android's large font sizes such a word can be wider than its space; instead of
 * breaking it across lines or cutting it off, the text shrinks only as much as it needs to: to 70 %
 * of its size at most, and with a large font never below the size it has at 100 %, so at 200 % it
 * can return to half its size.
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    val size = if (fontSize.isSpecified) fontSize else style.fontSize
    val smallest = minOf(0.7f, 1f / LocalDensity.current.fontScale)
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        // Twenty equal steps from the smallest to the full size, so a text that fits keeps exactly its
        // full size (a fixed 0.25 sp step from the smallest size can stop just short of it).
        autoSize = if (size.isSpecified) {
            TextAutoSize.StepBased(minFontSize = size * smallest, maxFontSize = size, stepSize = size * ((1f - smallest) / 20f))
        } else null,
    )
}

/**
 * Whether Android's font size is large (150 % or more). A few crowded places change their layout
 * then, rather than shrinking their text out of reach.
 */
@Composable
fun isLargeFontScale(): Boolean = LocalDensity.current.fontScale >= 1.5f
