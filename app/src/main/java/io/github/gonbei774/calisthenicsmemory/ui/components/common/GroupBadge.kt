package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * An exercise's group, shown on its row where the row is not already under the group's heading,
 * such as in search results (#278), so two exercises with the same name can be told apart.
 */
@Composable
fun GroupBadge(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
