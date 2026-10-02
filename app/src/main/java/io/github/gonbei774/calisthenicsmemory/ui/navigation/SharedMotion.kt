package io.github.gonbei774.calisthenicsmemory.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** The activity's shared-transition scope, around every screen change. */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The animated scope of the screen being shown or left. */
val LocalScreenAnimationScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Marks a chain's name so it glides between the place it was tapped (the catalogue list or a
 * Progressions card) and the chain screen's title. Outside a screen change it does nothing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedChainTitle(chainId: String): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val screen = LocalScreenAnimationScope.current ?: return this
    return with(shared) {
        this@sharedChainTitle.sharedBounds(
            rememberSharedContentState(key = "chain-title-$chainId"),
            animatedVisibilityScope = screen,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
        )
    }
}
