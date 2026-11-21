package io.github.gonbei774.calisthenicsmemory.util

/**
 * How long names and descriptions may be. Long enough for every catalogue name in every language
 * (longest: 47-character exercise, 39-character chain, 147-character description), and never a reason to
 * block text that is already stored: a field holding longer text, say from a shared file, can still be
 * edited and saved, as long as it doesn't grow.
 */
object TextLimits {
    const val EXERCISE_NAME = 60
    const val GROUP_NAME = 50
    const val DESCRIPTION = 200

    /** The most characters a field may hold: [limit], or the stored text's length when that is longer. */
    fun allowed(limit: Int, stored: String?): Int = maxOf(limit, stored?.length ?: 0)

    /** Whether [text] fits the field, given what it held when editing began. */
    fun fits(text: String, limit: Int, stored: String?): Boolean = text.length <= allowed(limit, stored)
}
