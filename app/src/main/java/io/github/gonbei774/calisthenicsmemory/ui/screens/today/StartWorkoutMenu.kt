package io.github.gonbei774.calisthenicsmemory.ui.screens.today

import androidx.activity.compose.BackHandler
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons

/**
 * Today's floating "start a workout" button, an Expressive FAB menu: it morphs into a close
 * button and fans out the four ways to train, so none is more than two taps away.
 */
@Composable
fun StartWorkoutMenu(
    onExercise: () -> Unit,
    onProgram: () -> Unit,
    onInterval: () -> Unit,
    onLogPast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }
    val label = stringResource(R.string.today_start_workout)

    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
                modifier = Modifier.semantics {
                    traversalIndex = -1f
                    contentDescription = label
                },
            ) {
                Icon(
                    if (checkedProgress > 0.5f) AppIcons.Close else AppIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        @Composable
        fun item(icon: ImageVector, text: String, action: () -> Unit) = FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                action()
            },
            icon = { Icon(icon, contentDescription = null) },
            text = { Text(text) },
        )
        item(AppIcons.RecordManually, stringResource(R.string.home_record), onLogPast)
        item(AppIcons.Interval, stringResource(R.string.interval_list_title), onInterval)
        item(AppIcons.Program, stringResource(R.string.today_kind_program), onProgram)
        item(AppIcons.Exercise, stringResource(R.string.exercise), onExercise)
    }
}
