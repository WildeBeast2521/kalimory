package io.github.gonbei774.calisthenicsmemory.data.v2

/**
 * A one-sided set's left value follows the right while the two are equal: the left text after the right side's
 * changed from [oldRight] to [newRight]. Once the left has been changed it stays as it is, so a person who uses one
 * value for both sides taps as often as before, and one who uses two is not overwritten.
 */
fun linkedLeft(oldRight: String, newRight: String, left: String): String = if (left == oldRight) newRight else left
