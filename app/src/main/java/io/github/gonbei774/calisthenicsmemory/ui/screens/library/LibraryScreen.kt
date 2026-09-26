package io.github.gonbei774.calisthenicsmemory.ui.screens.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
        DestinationEntry(Icons.Filled.Person, stringResource(R.string.home_create), onOpenExercises)
        DestinationEntry(Icons.AutoMirrored.Filled.List, stringResource(R.string.program_list_title), onOpenPrograms)
        DestinationEntry(Icons.Filled.Refresh, stringResource(R.string.interval_list_title), onOpenIntervals)
        DestinationEntry(Icons.Filled.Settings, stringResource(R.string.settings), onOpenSettings)
    }
}
