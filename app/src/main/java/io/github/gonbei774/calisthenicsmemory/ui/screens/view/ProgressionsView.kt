package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.progression.ChainProgress
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.screens.catalogue.standardText
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing

/**
 * The top of the Progressions tab (ADR 0005, decision 7): each followed chain with its current
 * step, how close the latest session came to the move-on standard, and the step after it.
 * The exercise targets that the Challenge tab showed follow below.
 */
internal fun LazyListScope.progressionsSection(
    progressions: List<ChainProgress>,
    onOpenChain: (String) -> Unit,
    onOpenCatalogue: () -> Unit,
) {
    item { ProgressionsHeading(stringResource(R.string.progressions_yours)) }
    if (progressions.isEmpty()) {
        item { EmptyProgressions(onOpenCatalogue) }
    } else {
        items(progressions, key = { it.chain.id }) { progress ->
            ChainCard(progress) { onOpenChain(progress.chain.id) }
        }
    }
    item { ProgressionsHeading(stringResource(R.string.progressions_targets)) }
}

@Composable
private fun ProgressionsHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
    )
}

@Composable
private fun EmptyProgressions(onOpenCatalogue: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                stringResource(R.string.progressions_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onOpenCatalogue) {
                Text(stringResource(R.string.progressions_browse))
            }
        }
    }
}

@Composable
private fun ChainCard(progress: ChainProgress, onClick: () -> Unit) {
    val steps = progress.chain.steps
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(progress.chain.name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.progressions_step_of, steps.indexOf(progress.step) + 1, steps.size),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(AppIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(progress.exercise.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            // Brass while working towards the standard, spruce once it has been met (ADR 0004).
            LinearProgressIndicator(
                progress = { progress.percent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = if (progress.mastered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Row {
                Text(
                    progress.lastSession?.let { stringResource(R.string.progressions_last, it.values.joinToString(", ")) }
                        ?: stringResource(R.string.progressions_not_trained),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.catalogue_move_on_at, standardText(progress.moveOn, progress.step.kind)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (progress.mastered && progress.next != null) {
                Text(
                    stringResource(R.string.progressions_ready),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                progress.next?.let { stringResource(R.string.progressions_next, stringResource(it.name)) }
                    ?: stringResource(R.string.progressions_top),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
