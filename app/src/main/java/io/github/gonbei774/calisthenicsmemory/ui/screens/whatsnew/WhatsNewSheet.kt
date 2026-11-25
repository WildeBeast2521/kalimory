package io.github.gonbei774.calisthenicsmemory.ui.screens.whatsnew

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.BuildConfig
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ChangeKind
import io.github.gonbei774.calisthenicsmemory.data.ChangeSection
import io.github.gonbei774.calisthenicsmemory.data.Changelog
import io.github.gonbei774.calisthenicsmemory.data.Release
import io.github.gonbei774.calisthenicsmemory.data.WhatsNewAssets
import io.github.gonbei774.calisthenicsmemory.ui.components.common.EmptyState
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SectionHeading
import io.github.gonbei774.calisthenicsmemory.ui.components.common.isLargeFontScale
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.RollingNumber
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.formatStoredDate
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class WhatsNewData(val releases: List<Release>, val summary: String?)

/**
 * The changes of this version, in a sheet from the bottom: after an update ([celebrate], with the
 * versions since [lastSeenVersion] marked new) or opened again from Settings. Reads the bundled
 * changelog off the main thread, so nothing is added to the app's start.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(lastSeenVersion: String?, celebrate: Boolean, onDismiss: () -> Unit) {
    val assets = LocalContext.current.assets
    val data by produceState<WhatsNewData?>(null) {
        value = withContext(Dispatchers.IO) {
            WhatsNewData(WhatsNewAssets.releases(assets, BuildConfig.VERSION_NAME), WhatsNewAssets.summary(assets, BuildConfig.VERSION_CODE))
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f, fill = false)) {
                val loaded = data
                when {
                    loaded == null -> Spacer(Modifier.height(160.dp))
                    loaded.releases.isEmpty() -> EmptyState(stringResource(R.string.whats_new_unreadable), Modifier.height(200.dp))
                    else -> WhatsNewList(loaded.releases, loaded.summary, lastSeenVersion, celebrate, visible = sheetState.currentValue == SheetValue.Expanded)
                }
            }
            Button(onClick = close, modifier = Modifier.fillMaxWidth().padding(Spacing.l).navigationBarsPadding()) {
                Text(stringResource(R.string.whats_new_close))
            }
        }
    }
}

@Composable
private fun animationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun WhatsNewList(
    releases: List<Release>,
    summary: String?,
    lastSeen: String?,
    celebrate: Boolean,
    visible: Boolean,
    installed: String = BuildConfig.VERSION_NAME,
) {
    val kalimory = releases.filter { it.isKalimory }
    val original = releases.filter { !it.isKalimory }
    val current = kalimory.firstOrNull()
    val others = kalimory.drop(1)
    val fresh = { r: Release -> celebrate && Changelog.isNew(r.version, lastSeen, installed) }
    var open by rememberSaveable { mutableStateOf<String?>(null) }

    // One deliberate moment when the sheet settles: the badge springs into place, the version rolls
    // from the one last seen to this one, and the first cards rise in. Nothing moves after that.
    val animate = animationsEnabled() && celebrate
    var entered by remember { mutableStateOf(!animate) }
    var shown by remember { mutableStateOf(if (animate && lastSeen != null) lastSeen else installed) }
    var played by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible && !played) {
            played = true
            entered = true
            delay(250)
            shown = installed
        }
    }
    if (!animate) shown = installed

    val large = isLargeFontScale()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        if (current != null) {
            item(key = "hero") { Hero(current, summary, shown, entered, animate, large) }
            sectionCards(current, entered, animate)
        }
        others.filter(fresh).forEach { release ->
            item(key = "new-${release.name}") { VersionHeader(release, isNew = true) }
            release.sections.forEach { section -> item(key = "${release.name}-${section.kind}") { SectionCard(section, release.name) } }
        }
        val earlier = others.filterNot(fresh)
        if (earlier.isNotEmpty()) {
            item(key = "earlier") { SectionHeading(stringResource(R.string.whats_new_earlier), Modifier.padding(top = Spacing.m)) }
            earlier.forEach { release -> item(key = release.name) { VersionRow(release, open == release.name) { open = if (open == release.name) null else release.name } } }
        }
        if (original.isNotEmpty()) {
            item(key = "before") { SectionHeading(stringResource(R.string.whats_new_before_kalimory), Modifier.padding(top = Spacing.m)) }
            original.forEach { release -> item(key = release.name) { VersionRow(release, open == release.name) { open = if (open == release.name) null else release.name } } }
        }
    }
}

private fun LazyListScope.sectionCards(release: Release, entered: Boolean, animate: Boolean) {
    release.sections.forEachIndexed { index, section ->
        item(key = "${release.name}-${section.kind}") { SectionCard(section, release.name, Modifier.entrance(index, entered, animate)) }
    }
}

/** The first few cards fade and rise 16 dp, 40 ms apart; with animations off they are simply there. */
@Composable
private fun Modifier.entrance(index: Int, entered: Boolean, animate: Boolean): Modifier {
    if (!animate) return this
    val progress by animateFloatAsState(if (entered) 1f else 0f, tween(320, delayMillis = index.coerceAtMost(3) * 40), label = "entrance")
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 16.dp.toPx()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun Hero(release: Release, summary: String?, shown: String, entered: Boolean, animate: Boolean, large: Boolean) {
    val spring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val scale by animateFloatAsState(if (entered) 1f else 0.7f, spring, label = "badge scale")
    val tilt by animateFloatAsState(if (entered) 0f else -20f, spring, label = "badge tilt")
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer { if (animate) { scaleX = scale; scaleY = scale; rotationZ = tilt } }
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(Spacing.l))
            Column {
                Text(
                    stringResource(R.string.whats_new_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
                RollingNumber(
                    text = shown,
                    style = if (large) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                release.date?.let {
                    Text(formatStoredDate(it, locale), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        summary?.let { Text(english(it), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) }
        if (locale.language != "en") {
            Text(stringResource(R.string.whats_new_english_only), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            release.sections.forEach { section ->
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                        Icon(section.kind.icon(), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(Spacing.xs))
                        Text("${section.entries.size} ${section.kind.label()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

/** A version that is new to this person but not the installed one: its name, date and a "New" label above its sections. */
@Composable
private fun VersionHeader(release: Release, isNew: Boolean) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        VersionTitle(release, isNew, MaterialTheme.typography.titleLarge, Modifier.weight(1f))
    }
}

/** A version's name, its labels and its date: on one line, or stacked at large text where one line would cut the name off. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VersionTitle(release: Release, isNew: Boolean, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val name = @Composable { mod: Modifier ->
        Text(release.name, style = style, color = MaterialTheme.colorScheme.onSurface, modifier = mod.semantics { heading() })
    }
    val labels = @Composable {
        if (isNew) Label(stringResource(R.string.whats_new_new))
        if (release.yanked) Label(stringResource(R.string.whats_new_withdrawn))
        release.date?.let {
            Text(formatStoredDate(it, locale), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = Spacing.s))
        }
    }
    if (isLargeFontScale()) {
        Column(modifier) {
            name(Modifier)
            FlowRow(verticalArrangement = Arrangement.Center, itemVerticalAlignment = Alignment.CenterVertically) { labels() }
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) { name(Modifier.weight(1f, fill = false)); labels() }
    }
}

@Composable
private fun Label(text: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.padding(start = Spacing.s)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs))
    }
}

/** An earlier version, collapsed to one row that opens in place. */
@Composable
private fun VersionRow(release: Release, expanded: Boolean, onToggle: () -> Unit) {
    val state = stringResource(if (expanded) R.string.whats_new_expanded else R.string.whats_new_collapsed)
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics(mergeDescendants = true) { stateDescription = state }
                .heightIn(min = 48.dp)
                .padding(horizontal = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VersionTitle(release, isNew = false, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(Modifier.padding(top = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                release.intro?.let { Text(english(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                release.sections.forEach { SectionCard(it, release.name) }
            }
        }
    }
}

/** A section of one version. A long one shows its first entries and a button for the rest, so a big release stays an overview. */
@Composable
private fun SectionCard(section: ChangeSection, owner: String, modifier: Modifier = Modifier) {
    var all by rememberSaveable("$owner-${section.kind}") { mutableStateOf(false) }
    val shown = if (all || section.entries.size <= COLLAPSED_ENTRIES + 1) section.entries else section.entries.take(COLLAPSED_ENTRIES)
    Surface(modifier.fillMaxWidth().animateContentSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { heading() }) {
                Icon(section.kind.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.s))
                Text(section.kind.label(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            shown.forEach { entry ->
                Row {
                    Text("\u2022", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(Spacing.s))
                    Text(english(entry), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
            }
            if (shown.size < section.entries.size) {
                OutlinedButton(onClick = { all = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.whats_new_show_all, section.entries.size))
                }
            }
        }
    }
}

private const val COLLAPSED_ENTRIES = 6

/** Change notes are written in English; marking them as such lets TalkBack read them with an English voice. */
private fun english(text: String) = buildAnnotatedString { withStyle(SpanStyle(localeList = LocaleList("en"))) { append(text) } }

@Composable
private fun ChangeKind.label() = stringResource(
    when (this) {
        ChangeKind.ADDED -> R.string.changelog_added
        ChangeKind.CHANGED -> R.string.changelog_changed
        ChangeKind.DEPRECATED -> R.string.changelog_deprecated
        ChangeKind.REMOVED -> R.string.changelog_removed
        ChangeKind.FIXED -> R.string.changelog_fixed
        ChangeKind.SECURITY -> R.string.changelog_security
    }
)

@Composable
private fun ChangeKind.icon() = when (this) {
    ChangeKind.ADDED -> AppIcons.Add
    ChangeKind.CHANGED -> AppIcons.Edit
    ChangeKind.DEPRECATED, ChangeKind.SECURITY -> AppIcons.Warning
    ChangeKind.REMOVED -> AppIcons.Remove
    ChangeKind.FIXED -> AppIcons.Check
}
