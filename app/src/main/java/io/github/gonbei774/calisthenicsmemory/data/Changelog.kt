package io.github.gonbei774.calisthenicsmemory.data

/** The kinds of change a CHANGELOG.md section can list, in Keep a Changelog's order. */
enum class ChangeKind(val heading: String) {
    ADDED("Added"), CHANGED("Changed"), DEPRECATED("Deprecated"), REMOVED("Removed"), FIXED("Fixed"), SECURITY("Security")
}

data class ChangeSection(val kind: ChangeKind, val entries: List<String>)

/** One `## [name] - date` block of CHANGELOG.md. [intro] is text before the first section, [entries] are shown as plain text. */
data class Release(
    val name: String,
    val date: String?,
    val yanked: Boolean,
    val intro: String?,
    val sections: List<ChangeSection>,
) {
    /** Releases of the app Kalimory was forked from are headed "Calisthenics Memory x.y.z". */
    val isKalimory: Boolean get() = !name.startsWith(ORIGINAL_APP)
    val version: String get() = name.removePrefix(ORIGINAL_APP).trim()

    companion object { const val ORIGINAL_APP = "Calisthenics Memory" }
}

/** Reads the bundled CHANGELOG.md for the "What's new" sheet. Lenient: unknown lines are skipped, nothing throws. */
object Changelog {
    private val heading = Regex("""^## \[([^\]]+)](?: - (\d{4}-\d{2}-\d{2}))?(.*)$""")
    private val prLink = Regex("""\s*\(\[#\d+]\([^)]*\)\)""")
    private val link = Regex("""\[([^\]]+)]\([^)]*\)""")

    /** Every dated release in the file, newest first as written; `[Unreleased]` is left out. */
    fun parse(text: String): List<Release> {
        val releases = mutableListOf<Release>()
        var name: String? = null; var date: String? = null; var yanked = false
        val intro = StringBuilder()
        var kind: ChangeKind? = null
        val sections = mutableListOf<ChangeSection>()
        var entries = mutableListOf<String>()

        fun closeSection() { kind?.let { if (entries.isNotEmpty()) sections += ChangeSection(it, entries) }; entries = mutableListOf(); kind = null }
        fun closeRelease() {
            closeSection()
            name?.takeIf { it != "Unreleased" }?.let { releases += Release(it, date, yanked, intro.toString().trim().ifEmpty { null }?.let(::plain), sections.toList()) }
            sections.clear(); intro.clear()
        }

        for (line in text.lineSequence()) {
            val h = heading.matchEntire(line)
            when {
                h != null -> { closeRelease(); name = h.groupValues[1]; date = h.groupValues[2].ifEmpty { null }; yanked = "YANKED" in h.groupValues[3] }
                name == null -> Unit
                line.startsWith("### ") -> { closeSection(); kind = ChangeKind.entries.firstOrNull { it.heading == line.removePrefix("### ").trim() } }
                // The link definitions at the bottom ("[1.1.1]: https://...") end the last release.
                line.startsWith("[") && "]: " in line -> Unit
                line.startsWith("- ") -> if (kind != null) entries += plain(line.removePrefix("- "))
                // A wrapped bullet continues on an indented line.
                line.startsWith("  ") && kind != null && entries.isNotEmpty() -> entries[entries.lastIndex] = entries.last() + " " + plain(line)
                kind == null && line.isNotBlank() -> intro.append(line.trim()).append(' ')
            }
        }
        closeRelease()
        return releases
    }

    /** Kalimory's releases up to and including [versionName] (a build between releases shows the last one), then the original app's. */
    fun upTo(releases: List<Release>, versionName: String): List<Release> =
        releases.filter { it.isKalimory && compare(it.version, versionName) <= 0 } + releases.filter { !it.isKalimory }

    /** Whether [version] is newer than what the person last saw; without a record only the installed one counts as new. */
    fun isNew(version: String, lastSeen: String?, installed: String): Boolean =
        if (lastSeen == null) version == installed else compare(version, lastSeen) > 0

    fun compare(a: String, b: String): Int {
        val x = a.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val c = (x.getOrElse(i) { 0 }).compareTo(y.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    /** An entry as a person reads it: no pull-request links, no Markdown marks. */
    fun plain(text: String): String = text
        .replace(prLink, "")
        .replace(link, "$1")
        .replace("**", "")
        .replace("`", "")
        .trim()
}

/** The sheet is due once the welcome guide is behind the person and the installed version is newer than the last one seen. */
fun whatsNewDue(welcomeSeen: Boolean, lastSeenCode: Int?, currentCode: Int): Boolean =
    welcomeSeen && (lastSeenCode == null || lastSeenCode < currentCode)
