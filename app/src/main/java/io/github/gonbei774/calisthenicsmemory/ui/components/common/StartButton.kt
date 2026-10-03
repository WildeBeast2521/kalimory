package io.github.gonbei774.calisthenicsmemory.ui.components.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons

/**
 * The compact Start on list rows: a pill with a play icon, like the Start on Today's hero card,
 * so every list starts a workout with the same button.
 */
@Composable
fun StartButton(onClick: () -> Unit, modifier: Modifier = Modifier, text: String = stringResource(R.string.start_workout)) {
    Button(onClick = onClick, modifier = modifier, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
        Icon(AppIcons.Play, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
