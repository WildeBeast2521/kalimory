package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmTopBar
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.chipColors
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import io.github.gonbei774.calisthenicsmemory.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit
) {
    val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
        topBar = { CalmTopBar(title = stringResource(R.string.open_source_licenses), onBack = onNavigateBack, scrollBehavior = topBarScroll) }
    ) { paddingValues ->
        // Generated at build time by the AboutLibraries Android plugin (res/raw/aboutlibraries).
        val libraries by produceLibraries()
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            // Version, license and funding chips in the app's own tonal colours, not the library's pink.
            colors = LibraryDefaults.libraryColors(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.onSurface,
                LibraryDefaults.chipColors(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer),
                LibraryDefaults.chipColors(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer),
                LibraryDefaults.chipColors(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer),
            )
        )
    }
}