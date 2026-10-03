package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The app's one − / + button beside a number: a tonal circle with an icon, the look of the large
 * buttons on the workout counter. Holding it keeps stepping while [onStep] returns true, so long
 * runs need no tapping. Every stepper in the app uses it, at [size] 40dp unless it sits beside a
 * big number.
 */
@Composable
fun NumberStepButton(
    increment: Boolean,
    contentDescription: String?,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    onStep: () -> Boolean,
) {
    val scope = rememberCoroutineScope()
    val currentOnStep by rememberUpdatedState(onStep)
    val cs = MaterialTheme.colorScheme
    val container = if (enabled) cs.secondaryContainer else cs.onSurface.copy(alpha = 0.12f)
    val content = if (enabled) cs.onSecondaryContainer else cs.onSurface.copy(alpha = 0.38f)
    Box(
        modifier = Modifier
            .size(size)
            .background(container, CircleShape)
            .semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
                if (!enabled) disabled()
                onClick { currentOnStep(); true }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val first = currentOnStep()
                    var repeatJob: Job? = null
                    try {
                        if (first) {
                            repeatJob = scope.launch {
                                delay(350)
                                while (isActive) {
                                    if (!currentOnStep()) break
                                    delay(80)
                                }
                            }
                        }
                        waitForUpOrCancellation()
                    } finally {
                        repeatJob?.cancel()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (increment) AppIcons.Add else AppIcons.Remove,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/**
 * − value unit +, the app's number input: the buttons step by [step] within [min]..[max] and the
 * number stays typeable. Used where a dialog asks for one number.
 */
@Composable
fun NumberStepper(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    step: Int = 1,
    min: Int = 0,
    max: Int = Int.MAX_VALUE,
) {
    val current = value.toIntOrNull() ?: min
    fun stepped(delta: Int): Boolean {
        val next = (current + delta).coerceIn(min, max)
        onValueChange(next.toString())
        return next in (min + 1) until max
    }
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberStepButton(increment = false, contentDescription = null, enabled = current > min, size = 48.dp) { stepped(-step) }
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.width(120.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = { text -> if (text.all { it.isDigit() } && (text.toIntOrNull() ?: 0) <= max) onValueChange(text) },
                modifier = Modifier.width(72.dp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            )
            androidx.compose.material3.Text(
                unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        NumberStepButton(increment = true, contentDescription = null, enabled = current < max, size = 48.dp) { stepped(step) }
    }
}
