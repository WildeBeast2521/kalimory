package io.github.gonbei774.calisthenicsmemory.ui.screens.library

import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.navigation.DestinationEntry
import io.github.gonbei774.calisthenicsmemory.ui.navigation.DestinationPage
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination

/** User-owned definitions (ADR 0001): exercises and groups, programs, intervals, and settings. */
@Composable
fun LibraryScreen(
    onOpenExercises: () -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenIntervals: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    DestinationPage(title = stringResource(PrimaryDestination.LIBRARY.label)) {
        DestinationEntry(AppIcons.Exercise, stringResource(R.string.home_create), onOpenExercises)
        DestinationEntry(AppIcons.Program, stringResource(R.string.program_list_title), onOpenPrograms)
        DestinationEntry(AppIcons.Interval, stringResource(R.string.interval_list_title), onOpenIntervals)
        DestinationEntry(AppIcons.Settings, stringResource(R.string.settings), onOpenSettings)
    }
}
