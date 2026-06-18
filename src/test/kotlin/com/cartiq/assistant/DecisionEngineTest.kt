package com.cartiq.assistant

import com.cartiq.assistant.engine.DecisionEngine
import com.cartiq.assistant.engine.NormalizationEngine
import com.cartiq.assistant.model.Platform
import com.cartiq.assistant.model.PriceResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DecisionEngineTest {

    private val normalizationEngine = NormalizationEngine()
    private val decisionEngine = DecisionEngine(normalizationEngine)

    private fun milkResult(platform: Platform, price: String) =
        PriceResult(platform, "Amul Gold Milk 500ml", "Amul", BigDecimal(price), "500ml")

    private fun breadResult(platform: Platform, price: String) =
        PriceResult(platform, "Britannia Bread 400g", "Britannia", BigDecimal(price), "400g")

    @Test
    fun `split cart picks cheapest per item`() {
        val results = mapOf(
            "milk" to listOf(
                milkResult(Platform.BLINKIT, "32.00"),
                milkResult(Platform.ZEPTO, "27.00"),  // cheapest
                milkResult(Platform.INSTAMART, "33.00")
            ),
            "bread" to listOf(
                breadResult(Platform.BLINKIT, "38.00"),  // cheapest
                breadResult(Platform.ZEPTO, "44.00"),
                breadResult(Platform.INSTAMART, "40.00")
            )
        )

        val decision = decisionEngine.decide(results)

        val splitCart = decision.splitCartPlan
        assertEquals(Platform.ZEPTO, splitCart.items["milk"]!!.platform)
        assertEquals(Platform.BLINKIT, splitCart.items["bread"]!!.platform)
        assertEquals(BigDecimal("65.00"), splitCart.total)
    }

    @Test
    fun `best single cart selects platform with lowest total for all items`() {
        val results = mapOf(
            "milk" to listOf(
                milkResult(Platform.BLINKIT, "32.00"),
                milkResult(Platform.ZEPTO, "27.00")
            ),
            "bread" to listOf(
                breadResult(Platform.BLINKIT, "38.00"),
                breadResult(Platform.ZEPTO, "44.00")
            )
        )
        // BLINKIT: 32+38=70, ZEPTO: 27+44=71 -> BLINKIT wins single cart
        val decision = decisionEngine.decide(results)

        assertNotNull(decision.bestSingleCart)
        val singlePlatform = decision.bestSingleCart!!.items.values.first().platform
        assertEquals(Platform.BLINKIT, singlePlatform)
    }

    @Test
    fun `empty results produce empty split cart`() {
        val decision = decisionEngine.decide(emptyMap())
        assertTrue(decision.splitCartPlan.items.isEmpty())
        assertEquals(BigDecimal.ZERO, decision.splitCartPlan.total)
    }

    @Test
    fun `per item comparisons have correct cheapest`() {
        val results = mapOf(
            "eggs" to listOf(
                PriceResult(Platform.BLINKIT, "Farm Eggs 6pcs", "", BigDecimal("55.00"), "6pcs"),
                PriceResult(Platform.ZEPTO, "Eggs White 6pcs", "", BigDecimal("52.00"), "6pcs"),
                PriceResult(Platform.INSTAMART, "Country Eggs 6pcs", "", BigDecimal("58.00"), "6pcs")
            )
        )

        val decision = decisionEngine.decide(results)
        val comparison = decision.perItemComparisons.first()
        assertEquals(Platform.ZEPTO, comparison.cheapest!!.platform)
        assertEquals(BigDecimal("52.00"), comparison.cheapest!!.price)
    }
}
