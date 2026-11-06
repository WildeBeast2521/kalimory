package io.github.gonbei774.calisthenicsmemory.data.catalogue

import java.text.Normalizer
import java.util.Locale

/**
 * Finds catalogue steps by the text the app shows (#258): step and chain names, muscles and equipment,
 * in the app's language. Pure, so it is tested on the JVM; the screen supplies the localized text.
 */
object CatalogueSearch {
    /** One step with the localized text it can be found by. [number] is its place in [chain], from 1. */
    data class Entry(
        val step: CatalogueStep,
        val chain: CatalogueChain,
        val number: Int,
        val name: String,
        val chainName: String,
        val muscles: List<String>,
        val equipment: List<String>,
    )

    /**
     * The steps matching [query], best first: the name itself, a name starting with it, every word in the
     * name, in the name and chain name, then anywhere including muscles and equipment. Ties keep [entries]'
     * order. A blank query finds nothing.
     */
    fun search(entries: List<Entry>, query: String): List<Entry> {
        val folded = fold(query).trim()
        if (folded.isEmpty()) return emptyList()
        val words = folded.split(WHITESPACE)
        return entries.mapIndexedNotNull { index, entry -> rank(entry, folded, words)?.let { Triple(it, index, entry) } }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .map { it.third }
    }

    private fun rank(entry: Entry, query: String, words: List<String>): Int? {
        val name = fold(entry.name)
        val withChain = name + " " + fold(entry.chainName)
        val everything = withChain + " " + (entry.muscles + entry.equipment).joinToString(" ") { fold(it) }
        return when {
            name == query -> 0
            name.startsWith(query) -> 1
            words.all { it in name } -> 2
            words.all { it in withChain } -> 3
            words.all { it in everything } -> 4
            else -> null
        }
    }

    /**
     * Text as search compares it: lower case, without accents or Arabic short vowels (harakat), with the
     * hamza and madda forms of alef as plain alef and full-width letters as ordinary ones. Japanese voicing
     * marks stay, so が is not か.
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

    private val WHITESPACE = Regex("\\s+")
}
