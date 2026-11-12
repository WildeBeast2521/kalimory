package io.github.gonbei774.calisthenicsmemory.data.catalogue

import io.github.gonbei774.calisthenicsmemory.util.SearchQuery
import io.github.gonbei774.calisthenicsmemory.util.rankedBy

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
        val q = SearchQuery(query)
        if (q.isBlank) return emptyList()
        return entries.rankedBy { q.rank(it.name, listOf(it.chainName), it.muscles + it.equipment) }
    }
}
