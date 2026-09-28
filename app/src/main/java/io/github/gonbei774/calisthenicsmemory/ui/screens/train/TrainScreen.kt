package io.github.gonbei774.calisthenicsmemory.ui.screens.train

import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
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
        DestinationEntry(AppIcons.Workout, stringResource(R.string.home_workout), onStartWorkout)
        DestinationEntry(AppIcons.Program, stringResource(R.string.program_list_title), onOpenPrograms)
        DestinationEntry(AppIcons.Interval, stringResource(R.string.interval_list_title), onOpenIntervals)
        DestinationEntry(AppIcons.RecordManually, stringResource(R.string.home_record), onRecordManually)
    }
}
