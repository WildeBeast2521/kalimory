package io.github.gonbei774.calisthenicsmemory.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

/** The activity's shared-transition scope, around every screen change. */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The animated scope of the screen being shown or left. */
val LocalScreenAnimationScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }
