package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons

/**
 * The red delete layer behind a swipeable card. It is drawn only while the card is being swiped:
 * at rest, a layer of the same shape under the card showed a faint red edge at its rounded corners.
 */
@Composable
fun SwipeToDeleteBackground(state: SwipeToDismissBoxState, shape: Shape) {
    if (state.dismissDirection != SwipeToDismissBoxValue.EndToStart) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error, shape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(AppIcons.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.onError)
    }
}
