package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.text.TextLayoutResult

/**
 * Every text on screen whose words do not fit the space it was given (cut off, or shortened with
 * "…"), as the large font sizes in Android's settings make it. One line per text.
 */
internal fun textFitProblems(compose: SemanticsNodeInteractionsProvider): List<String> {
    val texts = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
        .fetchSemanticsNodes()
    return texts.mapNotNull { node ->
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        val layout = results.firstOrNull() ?: return@mapNotNull null
        val shortened = (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
        // A line wider than the text's box, or below its bottom, is cut off on screen. Centred and
        // right-aligned lines are placed within the width offered, so compare widths, not edges.
        val cut = (0 until layout.lineCount).any { line ->
            layout.getLineRight(line) - layout.getLineLeft(line) > layout.size.width + 1 ||
                layout.getLineBottom(line) > layout.size.height + 1
        }
        // A word split across two lines ("Mo" / "n") means the space is too narrow for it.
        val source = layout.layoutInput.text.text
        val splitWord = (0 until layout.lineCount - 1).any { line ->
            val end = layout.getLineEnd(line)
            end in 1 until source.length && source[end - 1].isLetterOrDigit() && source[end].isLetterOrDigit()
        }
        if (!shortened && !cut && !splitWord) return@mapNotNull null
        val text = source.replace("\n", " ")
        val what = when {
            shortened -> "is shortened with …"
            splitWord -> "splits a word across lines"
            else -> "is cut off"
        }
        "\"${text.take(60)}\" $what"
    }
}
