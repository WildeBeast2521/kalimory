package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/*
 * The keyboard is applied once, here (#280). The activity resizes for the keyboard
 * (adjustResize), so Android no longer slides the window up; each screen's container takes the
 * keyboard's height as its bottom instead. A screen must not add its own imePadding(): counted twice,
 * the scroll area shrank by a second keyboard height, a focused field ended near the top and a blank
 * band sat above the keyboard. ConsistencyGuardTest keeps the keyboard inset in this file.
 */

/** The bottom of a screen: the keyboard while it is open, otherwise the system navigation bar. */
private val screenBottom: WindowInsets
    @Composable get() = WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom)

/**
 * Pads a screen above the keyboard or the system bar, whichever is taller, and consumes both. The
 * keyboard's inset is measured from the bottom of the screen, so imePadding adds only what the
 * system bar's padding has not already covered.
 */
fun Modifier.screenBottomInsets(): Modifier = navigationBarsPadding().imePadding()

/**
 * Pads above the keyboard only, for the home screens: their navigation bar clears the system bar
 * itself, and it is hidden while the keyboard is open ([keyboardVisible]).
 */
fun Modifier.keyboardInsets(): Modifier = imePadding()

/** Whether the keyboard is open, from the window's insets. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun keyboardVisible(): Boolean = WindowInsets.isImeVisible

/** The height of [screenBottomInsets], for things placed by hand above it, such as the snackbar. */
@Composable
fun screenBottomPadding(): Dp = screenBottom.asPaddingValues().calculateBottomPadding()
