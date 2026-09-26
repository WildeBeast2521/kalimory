package io.github.gonbei774.calisthenicsmemory.ui.screens.train

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.navigation.DestinationEntry
import io.github.gonbei774.calisthenicsmemory.ui.navigation.DestinationPage
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination

/** Every way to start or log training. Each entry opens an existing screen until the unified flow replaces it. */
@Composable
fun TrainScreen(
    onStartWorkout: () -> Unit,
    onRecordManually: () -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenIntervals: () -> Unit,
) {
    DestinationPage(title = stringResource(PrimaryDestination.TRAIN.label)) {
        DestinationEntry(Icons.Filled.PlayArrow, stringResource(R.string.home_workout), onStartWorkout)
        DestinationEntry(Icons.AutoMirrored.Filled.List, stringResource(R.string.program_list_title), onOpenPrograms)
        DestinationEntry(Icons.Filled.Refresh, stringResource(R.string.interval_list_title), onOpenIntervals)
        DestinationEntry(Icons.Filled.Create, stringResource(R.string.home_record), onRecordManually)
    }
}
