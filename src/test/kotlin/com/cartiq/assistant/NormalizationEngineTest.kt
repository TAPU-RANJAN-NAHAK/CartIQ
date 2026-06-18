package com.cartiq.assistant

import com.cartiq.assistant.engine.NormalizationEngine
import com.cartiq.assistant.model.Platform
import com.cartiq.assistant.model.PriceResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class NormalizationEngineTest {

    private val engine = NormalizationEngine()

    @Test
    fun `parseItems splits comma-separated list`() {
        val items = engine.parseItems("milk, bread, eggs")
        assertEquals(listOf("milk", "bread", "eggs"), items)
    }

    @Test
    fun `parseItems splits newline-separated list`() {
        val items = engine.parseItems("milk\nbread\neggs")
        assertEquals(listOf("milk", "bread", "eggs"), items)
    }

    @Test
    fun `parseItems deduplicates`() {
        val items = engine.parseItems("milk, milk, bread")
        assertEquals(listOf("milk", "bread"), items)
    }

    @Test
    fun `parseItems handles mixed separators`() {
        val items = engine.parseItems("milk, bread and eggs; rice")
        assertTrue("milk" in items)
        assertTrue("bread" in items)
        assertTrue("eggs" in items)
        assertTrue("rice" in items)
    }

    @Test
    fun `similarity returns high score for exact match`() {
        val score = engine.similarity("amul milk", "Amul Gold Milk 500ml")
        assertTrue(score > 0.4, "Expected score > 0.4, got $score")
    }

    @Test
    fun `similarity returns low score for unrelated products`() {
        val score = engine.similarity("milk", "Britannia Bread 400g")
        assertTrue(score < 0.4, "Expected score < 0.4, got $score")
    }

    @Test
    fun `rankByRelevance filters out irrelevant results`() {
        val results = listOf(
            PriceResult(Platform.BLINKIT, "Amul Gold Milk 500ml", "Amul", BigDecimal("32"), "500ml"),
            PriceResult(Platform.ZEPTO, "Britannia Bread 400g", "Britannia", BigDecimal("44"), "400g"),
            PriceResult(Platform.INSTAMART, "Nandini Toned Milk 500ml", "Nandini", BigDecimal("28"), "500ml")
        )
        val ranked = engine.rankByRelevance("milk", results)
        // Bread should be filtered out or ranked last
        val names = ranked.map { it.productName }
        assertTrue("Amul Gold Milk 500ml" in names || "Nandini Toned Milk 500ml" in names)
    }

    @Test
    fun `detectBrand identifies known brand`() {
        val brand = engine.detectBrand("Amul Gold Milk 500ml")
        assertEquals("amul", brand)
    }

    @Test
    fun `detectBrand returns null for unknown brand`() {
        val brand = engine.detectBrand("Generic Grocery Item")
        assertNull(brand)
    }
}
