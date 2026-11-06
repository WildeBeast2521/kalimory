package io.github.gonbei774.calisthenicsmemory.data.catalogue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Matching and ranking of the catalogue search (#258), on made-up text for real steps. */
class CatalogueSearchTest {
    private val push = Catalogue.chains.first { it.id == "push" }
    private val pull = Catalogue.chains.first { it.id != "push" }

    private fun entry(chain: CatalogueChain, index: Int, name: String, chainName: String, muscles: List<String> = emptyList(), equipment: List<String> = emptyList()) =
        CatalogueSearch.Entry(chain.steps[index], chain, index + 1, name, chainName, muscles, equipment)

    private val wall = entry(push, 0, "Wall push-up", "Push-up", muscles = listOf("Chest", "Triceps"), equipment = listOf("Wall"))
    private val diamond = entry(push, 4, "Diamond push-up", "Push-up", muscles = listOf("Triceps", "Chest"))
    private val hang = entry(pull, 0, "Dead hang", "Pull-up", muscles = listOf("Forearms"), equipment = listOf("Pull-up bar"))
    private val entries = listOf(wall, diamond, hang)

    private fun names(query: String, list: List<CatalogueSearch.Entry> = entries) = CatalogueSearch.search(list, query).map { it.name }

    @Test fun `a blank query finds nothing`() {
        assertTrue(names("").isEmpty())
        assertTrue(names("   ").isEmpty())
    }

    @Test fun `a name ranks above a muscle or equipment match`() {
        // "wall" is Wall push-up's name and nobody's equipment but its own; "triceps" only a muscle.
        assertEquals(listOf("Wall push-up"), names("wall"))
        assertEquals(listOf("Wall push-up", "Diamond push-up"), names("triceps"))
        val bar = entry(pull, 1, "Bar hang", "Pull-up")
        // Bar hang by name, then Dead hang by its equipment, the pull-up bar.
        assertEquals(listOf("Bar hang", "Dead hang"), names("bar", listOf(hang, bar)))
    }

    @Test fun `the exact name comes first, then names starting with the query`() {
        val diamondPlus = entry(push, 5, "Diamond push-up with pause", "Push-up")
        assertEquals(listOf("Diamond push-up", "Diamond push-up with pause"), names("diamond push-up", listOf(diamondPlus, diamond)))
    }

    @Test fun `every word must match, in any order and across name and chain`() {
        assertEquals(listOf("Diamond push-up"), names("push diamond"))
        assertEquals(listOf("Dead hang"), names("dead pull"))
        assertTrue(names("diamond pull").isEmpty())
    }

    @Test fun `ties keep the catalogue order`() {
        assertEquals(listOf("Wall push-up", "Diamond push-up"), names("push"))
    }

    @Test fun `case and accents are ignored`() {
        val french = entry(push, 2, "Pompe déclinée", "Pompe")
        assertEquals(listOf("Pompe déclinée"), names("DECLINEE", listOf(french)))
        assertEquals(listOf("Pompe déclinée"), names("déclinée", listOf(french)))
        val german = entry(push, 3, "Liegestütz", "Liegestütz")
        assertEquals(listOf("Liegestütz"), names("liegestutz", listOf(german)))
    }

    @Test fun `Arabic ignores short vowels and alef forms`() {
        val hangAr = entry(pull, 0, "التعلّق", "العقلة")
        val rowAr = entry(pull, 1, "التجديف الأفقي", "التجديف")
        assertEquals(listOf("التعلّق"), names("التعلق", listOf(hangAr, rowAr)))
        assertEquals(listOf("التجديف الأفقي"), names("الافقي", listOf(hangAr, rowAr)))
        assertEquals(listOf("التجديف الأفقي"), names("الإفقي", listOf(hangAr, rowAr)))
    }

    @Test fun `Japanese and Chinese match any part of a name`() {
        val ja = entry(push, 4, "ダイヤモンド腕立て伏せ", "腕立て伏せ")
        val zh = entry(push, 4, "钻石俯卧撑", "俯卧撑")
        assertEquals(listOf("ダイヤモンド腕立て伏せ"), names("腕立て", listOf(ja)))
        assertEquals(listOf("钻石俯卧撑"), names("钻石", listOf(zh)))
        assertEquals(listOf("钻石俯卧撑"), names("卧撑", listOf(zh)))
    }

    @Test fun `Japanese voicing marks still count`() {
        // ダ is タ with a voicing mark: different sounds, so タ alone must not find ダイヤモンド.
        val ja = entry(push, 4, "ダイヤモンド", "腕立て伏せ")
        assertTrue(names("タイヤ", listOf(ja)).isEmpty())
        assertEquals(listOf("ダイヤモンド"), names("ダイヤ", listOf(ja)))
    }

    @Test fun `full-width letters match ordinary ones`() {
        val l = entry(pull, 2, "L-sit", "Core")
        assertEquals(listOf("L-sit"), names("Ｌ－ｓｉｔ", listOf(l)))
    }
}
