package io.github.gonbei774.calisthenicsmemory.ui.screens.catalogue

import io.github.gonbei774.calisthenicsmemory.data.catalogue.recommendedRestSeconds
import io.github.gonbei774.calisthenicsmemory.ui.navigation.sharedChainTitle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueChain
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueStep
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Equipment
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Standard
import io.github.gonbei774.calisthenicsmemory.data.progression.Progressions
import io.github.gonbei774.calisthenicsmemory.data.progression.StepJourney
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.ui.components.muscles.MuscleMap
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.RowGroup
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.formatStoredDate
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

/** The built-in catalogue (ADR 0006): every chain, from where to begin to where it leads. */
@Composable
fun CatalogueScreen(viewModel: TrainingViewModel, onNavigateBack: () -> Unit, onOpenChain: (String) -> Unit) {
    val exercises by viewModel.exercises.collectAsState()
    val linked = remember(exercises) { exercises.mapNotNull { it.catalogId }.toSet() }
    CatalogueScaffold(stringResource(R.string.library_catalogue), onNavigateBack) {
        item {
            RowGroup {
                Catalogue.chains.forEach { chain ->
                    ChainRow(chain, inLibrary = chain.steps.count { it.id in linked }) { onOpenChain(chain.id) }
                }
            }
        }
    }
}

/** One chain's steps, easiest first; a step opens its details and can be added to the library. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueChainScreen(viewModel: TrainingViewModel, chainId: String, onNavigateBack: () -> Unit) {
    val chain = Catalogue.chain(chainId) ?: return
    val exercises by viewModel.exercises.collectAsState()
    val linked = remember(exercises) { exercises.mapNotNull { it.catalogId }.toSet() }
    var open by remember { mutableStateOf<CatalogueStep?>(null) }
    val chainName = stringResource(chain.name)

    val unfollowed by viewModel.unfollowedChains.collectAsState()
    val history by viewModel.history.collectAsState()
    val journey = remember(exercises, history) { Progressions.journey(chain, exercises, history).associateBy { it.step.id } }
    CatalogueScaffold(chainName, onNavigateBack, sharedChainId = chain.id) {
        // Following only matters once a step is in the library (ADR 0007, decision 4).
        if (chain.steps.any { it.id in linked }) {
            item { FollowRow(followed = chain.id !in unfollowed) { viewModel.setChainFollowed(chain.id, it) } }
        }
        item {
            RowGroup {
                chain.steps.forEach { step -> StepRow(step, step.id in linked, journey[step.id]) { open = step } }
            }
        }
    }

    open?.let { step ->
        val name = stringResource(step.name)
        val description = stringResource(step.description)
        ModalBottomSheet(onDismissRequest = { open = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            StepDetails(step, inLibrary = step.id in linked) {
                viewModel.addFromCatalogue(step, name, description, chainName)
                open = null
            }
        }
    }
}

@Composable
private fun CatalogueScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    // The chain whose name glides into this title from where it was tapped.
    sharedChainId: String? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(AppIcons.Back, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = (if (sharedChainId != null) Modifier.sharedChainTitle(sharedChainId) else Modifier).semantics { heading() },
            )
        }
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
            content = content,
        )
    }
}

@Composable
private fun ChainRow(chain: CatalogueChain, inLibrary: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(chain.name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.sharedChainTitle(chain.id),
            )
            // Where the chain begins and where it leads.
            Text(
                stringResource(chain.steps.first().name) + " → " + stringResource(chain.steps.last().name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (inLibrary > 0) {
                Text(
                    stringResource(R.string.catalogue_in_library_count, inLibrary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Icon(AppIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FollowRow(followed: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = followed, role = Role.Switch, onValueChange = onChange)
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.catalogue_follow), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(R.string.catalogue_follow_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.l))
            Switch(checked = followed, onCheckedChange = null)
        }
    }
}

@Composable
private fun LevelBadge(level: Int) {
    Box(
        Modifier.size(40.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text("$level", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun StepRow(step: CatalogueStep, inLibrary: Boolean, journey: StepJourney?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelBadge(step.difficulty)
        Spacer(Modifier.width(Spacing.l))
        Column(Modifier.weight(1f)) {
            Text(stringResource(step.name), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                standardText(step.working, step.kind) + " → " + standardText(step.moveOn, step.kind),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // The user's own climb, from stored dates only (phase 6 trends). Spruce once met (ADR 0004).
            if (journey != null) {
                val locale = LocalConfiguration.current.locales[0]
                val started = formatStoredDate(journey.firstOn, locale)
                Text(
                    journey.metOn?.let { stringResource(R.string.catalogue_journey_met, started, formatStoredDate(it, locale)) }
                        ?: stringResource(R.string.catalogue_journey_started, started),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (journey.metOn != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        if (inLibrary) {
            Icon(AppIcons.Check, contentDescription = stringResource(R.string.catalogue_in_library), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StepDetails(step: CatalogueStep, inLibrary: Boolean, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl).padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelBadge(step.difficulty)
            Spacer(Modifier.width(Spacing.l))
            Text(
                stringResource(step.name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(stringResource(step.description), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)

        Section(stringResource(R.string.catalogue_how_to)) {
            stringResource(step.cues).split("\n").forEachIndexed { index, cue ->
                Row {
                    Text("${index + 1}.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
                    Text(cue, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Decorative: the muscles are listed as text right below it.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MuscleMap(step.primaryMuscles, step.secondaryMuscles, Modifier.fillMaxWidth(0.75f))
        }
        Section(stringResource(R.string.catalogue_works)) { Muscles(step.primaryMuscles, strong = true) }
        if (step.secondaryMuscles.isNotEmpty()) {
            Section(stringResource(R.string.catalogue_also)) { Muscles(step.secondaryMuscles, strong = false) }
        }

        Section(stringResource(R.string.catalogue_needs)) {
            Text(
                if (step.equipment.isEmpty()) stringResource(R.string.catalogue_no_equipment)
                else step.equipment.map { equipmentLabel(it) }.joinToString(", "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Skills name the steps they build on (ADR 0005, decision 2): a hint, never a lock.
        val buildsOn = step.prerequisites.mapNotNull { Catalogue.step(it) }
        if (buildsOn.isNotEmpty()) {
            Section(stringResource(R.string.catalogue_builds_on)) {
                Text(
                    buildsOn.map { stringResource(it.name) }.joinToString(", "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Column {
            Text(stringResource(R.string.catalogue_start_at, standardText(step.working, step.kind)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.catalogue_move_on_at, standardText(step.moveOn, step.kind)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.catalogue_rest, step.recommendedRestSeconds()), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (inLibrary) {
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(AppIcons.Check, contentDescription = null)
                Spacer(Modifier.width(Spacing.s))
                Text(stringResource(R.string.catalogue_in_library))
            }
        } else {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(AppIcons.Add, contentDescription = null)
                Spacer(Modifier.width(Spacing.s))
                Text(stringResource(R.string.catalogue_add), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { heading() })
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Muscles(muscles: Set<Muscle>, strong: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        muscles.forEach { Chip(muscleLabel(it), strong) }
    }
}

/** The muscles doing the work in red, as the muscle map will show them (ADR 0008); helpers quieter. */
@Composable
private fun Chip(label: String, strong: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (strong) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (strong) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs))
    }
}

@Composable
internal fun standardText(standard: Standard, kind: ExerciseKind): String =
    stringResource(if (kind == ExerciseKind.ISOMETRIC) R.string.standard_seconds else R.string.standard_reps, standard.sets, standard.value)

@Composable
internal fun muscleLabel(muscle: Muscle): String = stringResource(
    when (muscle) {
        Muscle.ABDOMINALS -> R.string.muscle_abdominals
        Muscle.ABDUCTORS -> R.string.muscle_abductors
        Muscle.ADDUCTORS -> R.string.muscle_adductors
        Muscle.BICEPS -> R.string.muscle_biceps
        Muscle.CALVES -> R.string.muscle_calves
        Muscle.CHEST -> R.string.muscle_chest
        Muscle.FOREARMS -> R.string.muscle_forearms
        Muscle.GLUTES -> R.string.muscle_glutes
        Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
        Muscle.LATS -> R.string.muscle_lats
        Muscle.LOWER_BACK -> R.string.muscle_lower_back
        Muscle.MIDDLE_BACK -> R.string.muscle_middle_back
        Muscle.NECK -> R.string.muscle_neck
        Muscle.QUADRICEPS -> R.string.muscle_quadriceps
        Muscle.SHOULDERS -> R.string.muscle_shoulders
        Muscle.TRAPS -> R.string.muscle_traps
        Muscle.TRICEPS -> R.string.muscle_triceps
    }
)

@Composable
internal fun equipmentLabel(equipment: Equipment): String = stringResource(
    when (equipment) {
        Equipment.PULL_UP_BAR -> R.string.equipment_pull_up_bar
        Equipment.PARALLEL_BARS -> R.string.equipment_parallel_bars
        Equipment.BENCH -> R.string.equipment_bench
        Equipment.WALL -> R.string.equipment_wall
        Equipment.LOW_BAR -> R.string.equipment_low_bar
        Equipment.RESISTANCE_BAND -> R.string.equipment_resistance_band
        Equipment.SLIDER -> R.string.equipment_slider
        Equipment.ANCHOR -> R.string.equipment_anchor
        Equipment.POLE -> R.string.equipment_pole
        Equipment.RINGS -> R.string.equipment_rings
    }
)
