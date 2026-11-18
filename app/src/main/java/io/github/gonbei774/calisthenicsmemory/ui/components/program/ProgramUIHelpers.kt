package io.github.gonbei774.calisthenicsmemory.ui.components.program

import io.github.gonbei774.calisthenicsmemory.data.nextPendingIndex
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession

// 次の種目情報をテキスト1行で表示（実行中画面の下部用）
@Composable
fun NextExerciseText(
    session: ProgramExecutionSession,
    currentSetIndex: Int
) {
    // The set that will actually run next: the same rule afterSet() follows.
    val nextSet = nextPendingIndex(session.sets, currentSetIndex)?.let { session.sets[it] } ?: return

    val (_, nextExercise) = session.exercises[nextSet.exerciseIndex]
    val nextSideText = when (nextSet.side) {
        "Right" -> stringResource(R.string.side_right)
        "Left" -> stringResource(R.string.side_left)
        else -> null
    }

    val displayName = if (nextSideText != null) {
        "${nextExercise.name} - $nextSideText"
    } else {
        nextExercise.name
    }

    Text(
        text = "${stringResource(R.string.interval_next)}: $displayName",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp)
    )
}