package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A number that may be left blank, as a labelled stepper: blank shows a dash, stepping down from the lowest
 * value in [range] clears it, and stepping up from blank starts at the lowest value. Typing still works.
 */
@Composable
fun LabelledNumberStepper(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    range: IntRange,
    unit: String,
    modifier: Modifier = Modifier,
    step: Int = 1,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NumberStepper(
            value = value,
            onValueChange = { typed ->
                val n = typed.toIntOrNull()
                onValueChange(if (n == null || n < range.first) "" else n.coerceAtMost(range.last).toString())
            },
            unit = unit,
            step = step,
            // One step below the range is "blank", so stepping up from blank lands on the lowest value.
            min = range.first - step,
            max = range.last,
            placeholder = "–",
        )
    }
}
