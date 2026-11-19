package io.github.gonbei774.calisthenicsmemory.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import io.github.gonbei774.calisthenicsmemory.R

/**
 * One thing an import tells the person about a line or an item: what went wrong or what was left out.
 * The importers return these instead of English text, and the screen turns them into the app's language.
 * [res] is a string resource that takes [args] in order; [literal] is used as it is for the rare
 * message that comes from outside the app (an exception text).
 */
data class ImportIssue(
    @StringRes val res: Int,
    val args: List<Any> = emptyList(),
    val literal: String? = null,
) {
    /** The same issue for line [line] of a CSV file: the line number is the first argument. */
    fun atLine(line: Int) = copy(args = listOf<Any>(line) + args)

    fun text(context: Context): String = literal ?: context.getString(res, *args.toTypedArray())

    companion object {
        /** A message with no resource: [text] is shown as it is. */
        fun literal(text: String) = ImportIssue(R.string.import_issue_text, listOf(text), literal = text)
    }
}

/** Thrown inside a parser to say why one line cannot be read; carries the issue without its line number. */
internal class ImportIssueException(val issue: ImportIssue) : IllegalArgumentException()
