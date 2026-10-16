package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import android.os.Build
import io.github.gonbei774.calisthenicsmemory.BuildConfig
import java.io.File

/** What a crash report says about the app and the phone, besides the error itself. */
data class CrashContext(
    val versionName: String,
    val versionCode: Int,
    val androidRelease: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
) {
    companion object {
        fun current() = CrashContext(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
        )
    }
}

/**
 * Crash reports kept on the phone. Kalimory has no internet permission, so a crash is written to
 * app storage and offered for sharing on the next launch; nothing is sent unless the user shares
 * it. A report holds the app version, the Android version, the phone model, the thread and the
 * stack trace, nothing else. Only the newest [keep] are kept.
 */
class CrashReports(private val directory: File, private val keep: Int = KEEP) {

    /** Writes a report for [error]. Never throws: it runs while the app is crashing. */
    fun record(threadName: String, error: Throwable, context: CrashContext, atMillis: Long): Boolean = try {
        directory.mkdirs()
        File(directory, "$PREFIX$atMillis$SUFFIX").writeText(format(threadName, error, context))
        prune()
        true
    } catch (_: Throwable) {
        false
    }

    /** The kept reports' text, newest first. */
    fun pending(): List<String> = files().mapNotNull { runCatching { it.readText() }.getOrNull() }

    /** Deletes every kept report. */
    fun clear() {
        files().forEach { it.delete() }
    }

    private fun prune() {
        files().drop(keep).forEach { it.delete() }
    }

    /** Report files, newest first (their names carry the time they were written). */
    private fun files(): List<File> =
        directory.listFiles { file -> file.name.startsWith(PREFIX) && file.name.endsWith(SUFFIX) }.orEmpty()
            .sortedByDescending { it.name.removePrefix(PREFIX).removeSuffix(SUFFIX).toLongOrNull() ?: Long.MIN_VALUE }

    companion object {
        const val KEEP = 3
        const val DIRECTORY = "crash-reports"
        const val ISSUES_URL = "https://github.com/WildeBeast2521/kalimory/issues/new"

        /** A long trace (a stack overflow) is cut, so three reports stay well within a share intent. */
        const val MAX_TRACE_CHARS = 32_000
        private const val PREFIX = "crash-"
        private const val SUFFIX = ".txt"

        fun forApp(context: Context) = CrashReports(File(context.filesDir, DIRECTORY))

        /** One report, ready to paste into an issue (Markdown). English: it is read by the developers. */
        fun format(threadName: String, error: Throwable, context: CrashContext): String {
            val trace = error.stackTraceToString().trimEnd().let {
                if (it.length <= MAX_TRACE_CHARS) it else it.take(MAX_TRACE_CHARS) + "\n\t… (cut)"
            }
            return buildString {
                appendLine("**Kalimory crash report**")
                appendLine()
                appendLine("- App: ${context.versionName} (${context.versionCode})")
                appendLine("- Android: ${context.androidRelease} (API ${context.sdkInt})")
                appendLine("- Device: ${context.manufacturer} ${context.model}".trimEnd())
                appendLine("- Thread: $threadName")
                appendLine()
                appendLine("```")
                appendLine(trace)
                append("```")
            }
        }

        /** The text the share sheet carries: every kept report, newest first, then where to file it. */
        fun shareText(reports: List<String>): String =
            reports.joinToString(separator = "\n\n---\n\n", postfix = "\n\nReport it at $ISSUES_URL\n")
    }
}
