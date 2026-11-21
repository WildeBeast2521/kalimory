package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Every element on screen that can be tapped, checked for what TalkBack and touch need: a label (a
 * text, a description or a state) and a touch area of at least 48 × 48 dp. Returns one line per
 * problem, empty when there is none.
 */
internal fun tapTargetProblems(compose: SemanticsNodeInteractionsProvider, density: Density): List<String> {
    val minPx = with(density) { 48.dp.toPx() } - 0.5f
    val nodes = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
    return touchingButtons(nodes, density) + nodes.mapNotNull { node ->
        val config = node.config
        val label = listOfNotNull(
            config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
            config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
            config.getOrNull(SemanticsProperties.StateDescription),
        ).joinToString(" ").trim()
        // An element cut off by scrolling is clipped, so its size is not measured.
        val shape = node.boundsInRoot
        val clipped = shape.width < node.size.width - 1 || shape.height < node.size.height - 1
        val touch = if (clipped) null else effectiveTouchArea(shape, node.touchBoundsInRoot, nodes.map { it.boundsInRoot })
        val found = listOfNotNull(
            "no label".takeIf { label.isEmpty() },
            touch?.let { "touch area ${it.width.toInt()}×${it.height.toInt()} px, under 48 dp" }
                ?.takeIf { touch.width < minPx || touch.height < minPx },
        )
        if (found.isEmpty()) null else "\"${label.take(50)}\" — ${found.joinToString(", ")}"
    }
}

/**
 * The area that really answers a touch. Compose widens a small target towards 48 dp ([widened]),
 * but where a neighbouring target's shape is nearer, a touch in between goes to the nearer one, so
 * the widened area stops halfway to each neighbour. Targets inside one another (a button on a
 * card) are not neighbours.
 */
internal fun effectiveTouchArea(shape: Rect, widened: Rect, others: List<Rect>): Rect {
    var left = widened.left; var top = widened.top; var right = widened.right; var bottom = widened.bottom
    for (other in others) {
        if (other == shape || other.contains(shape) || shape.contains(other)) continue
        val sameRow = other.top < bottom && other.bottom > top
        val sameColumn = other.left < right && other.right > left
        if (sameRow && other.left >= shape.right && other.left < right) right = maxOf(shape.right, (shape.right + other.left) / 2)
        if (sameRow && other.right <= shape.left && other.right > left) left = minOf(shape.left, (shape.left + other.right) / 2)
        if (sameColumn && other.top >= shape.bottom && other.top < bottom) bottom = maxOf(shape.bottom, (shape.bottom + other.top) / 2)
        if (sameColumn && other.bottom <= shape.top && other.bottom > top) top = minOf(shape.top, (shape.top + other.bottom) / 2)
    }
    return Rect(left, top, right, bottom)
}

private fun Rect.contains(other: Rect) =
    left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom

/**
 * Two buttons side by side or one above the other need a gap (4 dp at least), or a press can land on the wrong
 * one and the pair reads as one block. Buttons inside one another are not neighbours; segmented choices are
 * not buttons (they are radio buttons and tabs), and a strip of five or more equal cells (the calendar) is flush by design.
 */
internal fun touchingButtons(nodes: List<SemanticsNode>, density: Density, minGapDp: Float = 4f): List<String> {
    val gap = with(density) { minGapDp.dp.toPx() }
    val buttons = nodes.filter { it.config.getOrNull(SemanticsProperties.Role) == Role.Button }
        .filter { n -> n.boundsInRoot.width >= n.size.width - 1 && n.boundsInRoot.height >= n.size.height - 1 }
    fun label(n: SemanticsNode) = n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
        ?: n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ") ?: "?"
    fun stripCells(r: androidx.compose.ui.geometry.Rect) = buttons.count { kotlin.math.abs(it.boundsInRoot.top - r.top) < 2f && kotlin.math.abs(it.boundsInRoot.height - r.height) < 2f }
    val problems = mutableListOf<String>()
    for (i in buttons.indices) for (j in i + 1 until buttons.size) {
        val a = buttons[i].boundsInRoot; val b = buttons[j].boundsInRoot
        if (a.contains(b) || b.contains(a)) continue
        // A strip or grid of five or more cells in one row (the calendar's days) is flush by design.
        if (stripCells(a) >= 5 && kotlin.math.abs(a.top - b.top) < 2f && kotlin.math.abs(a.height - b.height) < 2f) continue
        val horizontalGap = maxOf(b.left - a.right, a.left - b.right)
        val verticalGap = maxOf(b.top - a.bottom, a.top - b.bottom)
        // Touching or overlapping: closer than the gap on both axes at once.
        if (horizontalGap < gap && verticalGap < gap) {
            problems += "\"${label(buttons[i]).take(30)}\" and \"${label(buttons[j]).take(30)}\" touch (gap ${maxOf(horizontalGap, verticalGap).toInt()} px, under 4 dp)"
        }
    }
    return problems
}
