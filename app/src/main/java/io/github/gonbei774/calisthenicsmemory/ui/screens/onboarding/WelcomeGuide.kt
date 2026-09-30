package io.github.gonbei774.calisthenicsmemory.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.TimerDial
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutTone
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.ShapeBadge
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4
private val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * A short welcome: what the app is, its four places, how a workout runs, and that the data stays
 * on the phone. Shown once on first launch and again from Settings. It asks for nothing, and
 * Skip is always there.
 */
@Composable
fun WelcomeGuide(onFinish: () -> Unit, onAddExercise: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGE_COUNT - 1
    // Back steps to the previous page; on the first page it leaves as usual.
    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!last) TextButton(onClick = onFinish) { Text(stringResource(R.string.guide_skip)) }
            else Spacer(Modifier.height(48.dp))
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val position = stringResource(R.string.guide_page_position, page + 1, PAGE_COUNT)
            GuidePage(visible = pager.currentPage == page, position = position) {
                when (page) {
                    0 -> WelcomePage()
                    1 -> PlacesPage()
                    2 -> WorkoutPage()
                    else -> DataPage()
                }
            }
        }
        PageDots(pager.currentPage)
        Spacer(Modifier.height(Spacing.l))
        if (last) {
            Button(onClick = onAddExercise, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.guide_add_exercise), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(Spacing.s))
            OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.guide_go_to_today), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.guide_next), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** A page whose picture settles in when the page arrives. */
@Composable
private fun GuidePage(visible: Boolean, position: String, content: @Composable () -> Unit) {
    val arrival = remember { Animatable(0f) }
    LaunchedEffect(visible) { if (visible) arrival.animateTo(1f, tween(450, easing = Decelerate)) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = position }
            .graphicsLayer {
                alpha = 0.4f + 0.6f * arrival.value
                translationY = (1f - arrival.value) * 16.dp.toPx()
            },
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Spacer(Modifier.height(Spacing.s))
        content()
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Emblem(icon: ImageVector) {
    Box(
        Modifier
            .size(96.dp)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun Item(icon: ImageVector, name: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ShapeBadge(icon, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.width(Spacing.l))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WelcomePage() {
    Emblem(AppIcons.Workout)
    Title(stringResource(R.string.guide_welcome_title))
    Body(stringResource(R.string.guide_welcome_body))
}

@Composable
private fun PlacesPage() {
    Title(stringResource(R.string.guide_places_title))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Item(AppIcons.Today, stringResource(R.string.nav_today), stringResource(R.string.guide_place_today))
        Item(AppIcons.Train, stringResource(R.string.nav_train), stringResource(R.string.guide_place_train))
        Item(AppIcons.Progress, stringResource(R.string.nav_progress), stringResource(R.string.guide_place_progress))
        Item(AppIcons.Library, stringResource(R.string.nav_library), stringResource(R.string.guide_place_library))
    }
}

@Composable
private fun WorkoutPage() {
    // The real timer ring, as a picture: the same thing the user will see in a workout.
    Box(Modifier.fillMaxWidth().clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        TimerDial(progress = 0.7f, value = "12", accent = WorkoutTone.work, paused = false, onToggle = {}, size = 200.dp)
    }
    Title(stringResource(R.string.guide_workout_title))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Item(AppIcons.Timer, stringResource(R.string.guide_workout_sets_title), stringResource(R.string.guide_workout_sets))
        Item(AppIcons.Play, stringResource(R.string.guide_workout_pause_title), stringResource(R.string.guide_workout_pause))
        Item(AppIcons.Check, stringResource(R.string.guide_workout_summary_title), stringResource(R.string.guide_workout_summary))
    }
}

@Composable
private fun DataPage() {
    Emblem(AppIcons.Save)
    Title(stringResource(R.string.guide_data_title))
    // The path uses the labels the user will actually see there.
    val backupLabel = stringResource(R.string.section_full_backup).substringBefore(" (").substringBefore("（")
    Body(stringResource(R.string.guide_data_body, stringResource(R.string.settings) + " › " + backupLabel))
}

@Composable
private fun PageDots(current: Int) {
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally),
    ) {
        repeat(PAGE_COUNT) { index ->
            Box(
                Modifier
                    .size(width = if (index == current) 24.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}
