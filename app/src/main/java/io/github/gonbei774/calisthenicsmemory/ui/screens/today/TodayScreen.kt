package io.github.gonbei774.calisthenicsmemory.ui.screens.today

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.navigation.DestinationEntry
import io.github.gonbei774.calisthenicsmemory.ui.navigation.DestinationPage
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import io.github.gonbei774.calisthenicsmemory.ui.screens.TodayDashboardCard
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import java.time.LocalDate

@Composable
fun TodayScreen(
    viewModel: TrainingViewModel,
    onOpenToDo: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val exercises by viewModel.exercises.collectAsState()
    val records by viewModel.records.collectAsState()
    val todayDate = LocalDate.now().toString()
    val todayRecords = remember(records, todayDate) { records.filter { it.date == todayDate } }

    DestinationPage(
        title = stringResource(PrimaryDestination.TODAY.label),
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
            }
        },
    ) {
        DestinationEntry(Icons.Filled.CheckCircle, stringResource(R.string.todo_title), onOpenToDo)
        TodayDashboardCard(records = todayRecords, exercises = exercises, onNavigateToView = onOpenHistory)
    }
}
