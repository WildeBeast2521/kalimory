package io.github.gonbei774.calisthenicsmemory.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import io.github.gonbei774.calisthenicsmemory.Screen

/**
 * How far a screen sits from the primary destinations. Moving to a deeper screen is forward, and
 * moving to a shallower one is back, so the slide direction matches where the user went.
 */
internal fun Screen.depth(): Int = when (this) {
    Screen.Home -> 0
    Screen.ToDo, Screen.Create, Screen.Settings, Screen.ProgramList, Screen.IntervalList, Screen.Catalogue, Screen.WeeklyReview -> 1
    is Screen.CatalogueChain -> if (fromHome) 1 else 2
    is Screen.Record -> if (fromToDo) 2 else 1
    is Screen.Workout -> if (fromToDo && !fromToday) 2 else 1
    is Screen.ProgramExecution -> if (fromToday) 1 else 2
    is Screen.IntervalExecution -> if (fromToday) 1 else 2
    is Screen.ProgramEdit, is Screen.IntervalEdit,
    Screen.Licenses, Screen.Backup, Screen.CsvDataManagement, Screen.ShareHub -> 2
    Screen.CommunityShareExport -> 3
    // Always deeper than where the workout returned, so it slides in and Done slides back.
    is Screen.WorkoutSummary -> 4
}

// Material's emphasized curves: entering content decelerates, leaving content accelerates.
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** Forward slides the new screen in from the end; back reverses it. */
internal fun screenTransition(forward: Boolean): ContentTransform {
    val sign = if (forward) 1 else -1
    val enter = slideInHorizontally(tween(400, easing = EmphasizedDecelerate)) { width -> sign * width * 3 / 10 } +
        fadeIn(tween(250, delayMillis = 50, easing = EmphasizedDecelerate))
    val exit = slideOutHorizontally(tween(250, easing = EmphasizedAccelerate)) { width -> -sign * width / 5 } +
        fadeOut(tween(150, easing = EmphasizedAccelerate))
    return enter togetherWith exit
}

/** Tabs are peers, so they fade through instead of sliding. */
internal fun tabTransition(): ContentTransform =
    (fadeIn(tween(210, delayMillis = 90, easing = EmphasizedDecelerate)) +
        scaleIn(tween(210, delayMillis = 90, easing = EmphasizedDecelerate), initialScale = 0.96f)) togetherWith
        fadeOut(tween(90, easing = EmphasizedAccelerate))

/** Identifies a screen and its arguments, for keeping its saved state while it is on the way back. */
internal fun Screen.stateKey(): String = toString()

/**
 * Stops taps from reaching a screen once it has started to leave. [AnimatedContent] keeps the old
 * screen on display while it animates out, and it would still take input: a double tap on
 * "Record workout" would save the workout twice. Read in composition, so it follows the transition.
 */
internal fun Modifier.ignoreInputWhileLeaving(scope: AnimatedVisibilityScope): Modifier =
    if (scope.transition.targetState != EnterExitState.PostExit) this
    else pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                // The first pass runs parent first, so no child sees an unconsumed tap.
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    }
