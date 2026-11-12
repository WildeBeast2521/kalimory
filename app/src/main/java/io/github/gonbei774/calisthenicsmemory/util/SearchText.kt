package io.github.gonbei774.calisthenicsmemory.util

import java.text.Normalizer
import java.util.Locale

/**
 * How every search in the app compares text (#258, #278): one matcher, so the catalogue, the library,
 * the exercise pickers and the program lists find the same things the same way, in every language.
 */
class SearchQuery(raw: String) {
    private val folded = fold(raw).trim()
    private val words = if (folded.isEmpty()) emptyList() else folded.split(WHITESPACE)

    /** True for a blank query, which finds nothing (or leaves a list as it is, as each screen decides). */
    val isBlank: Boolean get() = words.isEmpty()

    /**
     * How well [name] matches, best first, or null when it does not: 0 the name itself, 1 a name
     * starting with the query, 2 every word in the name, then 3, 4… every word in the name and the
     * first one, two… of [more] (such as a group's name, or a step's chain, then its muscles).
     */
    fun rank(name: String, vararg more: List<String>): Int? {
        if (words.isEmpty()) return null
        val foldedName = fold(name)
        if (foldedName == folded) return 0
        if (foldedName.startsWith(folded)) return 1
        if (words.all { it in foldedName }) return 2
        var text = foldedName
        more.forEachIndexed { index, tier ->
            text += " " + tier.joinToString(" ") { fold(it) }
            if (words.all { it in text }) return 3 + index
        }
        return null
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")

        /**
         * Text as search compares it: lower case, without accents or Arabic short vowels (harakat),
         * with the hamza and madda forms of alef as plain alef and full-width letters as ordinary ones.
         * Japanese voicing marks stay, so が is not か. Japanese and Chinese match any part of a name,
         * since every word is matched as a substring.
         */
        fun fold(text: String): String {
            val decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD)
            val out = StringBuilder(decomposed.length)
            for (c in decomposed) {
                when {
                    c == '゙' || c == '゚' -> out.append(c)
                    Character.getType(c) == Character.NON_SPACING_MARK.toInt() -> Unit
                    c == 'ٱ' -> out.append('ا') // alef wasla
                    c == 'ـ' -> Unit // tatweel, a stretch with no letter
                    else -> out.append(c)
                }
            }
            return out.toString().lowercase(Locale.ROOT)
        }
    }
}

/** The items [rank] matches, best rank first; equal ranks keep the list's own order. */
internal inline fun <T> List<T>.rankedBy(rank: (T) -> Int?): List<T> =
    mapIndexedNotNull { index, item -> rank(item)?.let { Triple(it, index, item) } }
        .sortedWith(compareBy({ it.first }, { it.second }))
        .map { it.third }
