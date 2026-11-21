package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * A measurement (reps, seconds, centimetres, kilograms) as a labelled stepper: the label above, then "− value +".
 * The buttons step the number by [step]; the number stays typeable, and whatever is typed goes to [onValueChange]
 * unchanged, so the screen keeps its own parsing. With [decimals] the value is shown with one decimal (12.5).
 * A blank value counts as 0, and [min] is the lowest the buttons go (null: no lowest, for a distance that may be
 * negative).
 */
@Composable
fun MetricStepper(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
    decimals: Boolean = false,
    step: Double = 1.0,
    min: Double? = 0.0,
) {
    val current = value.replace(',', '.').toDoubleOrNull() ?: 0.0
    fun stepped(delta: Double): Boolean {
        val next = (current + delta).let { if (min != null) it.coerceAtLeast(min) else it }
        onValueChange(if (decimals) String.format(Locale.ROOT, "%.1f", next) else next.toInt().toString())
        return min == null || next > min
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            NumberStepButton(increment = false, contentDescription = null, enabled = min == null || current > min, size = 48.dp) { stepped(-step) }
            Box(Modifier.width(120.dp), contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.width(96.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.Center) {
                            if (value.isEmpty()) Text("–", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            field()
                        }
                    },
                )
            }
            NumberStepButton(increment = true, contentDescription = null, size = 48.dp) { stepped(step) }
        }
    }
}
