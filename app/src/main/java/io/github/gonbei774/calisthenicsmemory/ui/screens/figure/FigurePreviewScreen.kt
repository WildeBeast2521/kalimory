package io.github.gonbei774.calisthenicsmemory.ui.screens.figure

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.figure.SampleMotions
import io.github.gonbei774.calisthenicsmemory.data.figure.StepMotions
import io.github.gonbei774.calisthenicsmemory.ui.components.figure.FigureStyle
import io.github.gonbei774.calisthenicsmemory.ui.components.figure.FigureView
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/**
 * A debug-only screen for authoring demonstrations (ADR 0008, decision 3): pick a motion, turn the
 * camera, pause on a pose. Its text is English only because users never see it.
 */
@Composable
fun FigurePreviewScreen(onNavigateBack: () -> Unit, initialStep: String? = null, initialStyle: String? = null) {
    // The samples, then every catalogue step that has a motion, named by its id.
    val samples = remember {
        SampleMotions.all + StepMotions.stepIds.sorted().mapNotNull { id ->
            val step = Catalogue.step(id) ?: return@mapNotNull null
            SampleMotions.Sample(id, StepMotions.forStep(id)!!, step.primaryMuscles, step.secondaryMuscles)
        }
    }
    var selected by remember { mutableIntStateOf(samples.indexOfFirst { it.name == initialStep }.coerceAtLeast(0)) }
    val sample = samples[selected]
    val motion = sample.motion
    var yaw by remember(selected) { mutableFloatStateOf(motion.camera.yaw) }
    var pitch by remember(selected) { mutableFloatStateOf(motion.camera.pitch) }
    var playing by remember { mutableStateOf(true) }
    var style by remember { mutableStateOf(FigureStyle.entries.firstOrNull { it.name == initialStyle } ?: FigureStyle.SHADED) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(AppIcons.Back, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Figure preview", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            samples.forEachIndexed { index, item ->
                FilterChip(selected = index == selected, onClick = { selected = index }, label = { Text(item.name) })
            }
        }
        FigureView(
            motion,
            Modifier.fillMaxWidth().weight(1f).padding(Spacing.l),
            primary = sample.primary,
            secondary = sample.secondary,
            playing = playing,
            style = style,
            camera = motion.camera.copy(yaw = yaw, pitch = pitch),
        )
        Column(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m)) {
            Text("Camera turn: ${yaw.toInt()}°", style = MaterialTheme.typography.bodyMedium)
            Slider(value = yaw, onValueChange = { yaw = it }, valueRange = -90f..90f)
            Text("Camera tilt: ${pitch.toInt()}°", style = MaterialTheme.typography.bodyMedium)
            Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0f..45f)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                FigureStyle.entries.forEach { option ->
                    FilterChip(selected = option == style, onClick = { style = option }, label = { Text(option.name.lowercase()) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Play", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = playing, onCheckedChange = { playing = it })
            }
        }
    }
}
