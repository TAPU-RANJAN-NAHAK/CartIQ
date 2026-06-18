package com.cartiq.assistant

import com.cartiq.assistant.formatter.ResponseFormatter
import com.cartiq.assistant.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ResponseFormatterTest {

    private val formatter = ResponseFormatter()

    private fun makeDecisionResult(): DecisionResult {
        val milkResults = listOf(
            PriceResult(Platform.BLINKIT, "Amul Gold Milk 500ml", "Amul", BigDecimal("32.00"), "500ml", deepLink = "https://blinkit.com/milk"),
            PriceResult(Platform.ZEPTO, "Nandini Milk 500ml", "Nandini", BigDecimal("27.00"), "500ml", deepLink = "https://zepto.com/milk")
        )
        val breadResults = listOf(
            PriceResult(Platform.BLINKIT, "Modern Bread 400g", "Modern", BigDecimal("38.00"), "400g", deepLink = "https://blinkit.com/bread"),
            PriceResult(Platform.ZEPTO, "Harvest Gold Bread", "Harvest Gold", BigDecimal("36.00"), "400g", deepLink = "https://zepto.com/bread")
        )

        val comparisons = listOf(
            ItemComparison("milk", milkResults, milkResults.minByOrNull { it.price }),
            ItemComparison("bread", breadResults, breadResults.minByOrNull { it.price })
        )

        val splitItems = mapOf(
            "milk" to milkResults.minByOrNull { it.price }!!,
            "bread" to breadResults.minByOrNull { it.price }!!
        )
        val splitTotal = splitItems.values.sumOf { it.price }
        val splitLinks = splitItems.values.groupBy { it.platform }.mapValues { (_, v) -> v.first().deepLink }
        val splitCart = CartPlan(CartPlanType.SPLIT_CART, splitItems, splitTotal, splitLinks)

        return DecisionResult(comparisons, null, splitCart)
    }

    @Test
    fun `formatted output contains price comparison header`() {
        val result = makeDecisionResult()
        val output = formatter.formatDecisionResult(result)
        assertTrue(output.contains("Price Comparison"), "Should contain 'Price Comparison'")
    }

    @Test
    fun `formatted output contains total`() {
        val result = makeDecisionResult()
        val output = formatter.formatDecisionResult(result)
        assertTrue(output.contains("Total"), "Should contain 'Total'")
        assertTrue(output.contains("63"), "Should contain total amount 63.00")
    }

    @Test
    fun `formatted output marks cheapest with checkmark`() {
        val result = makeDecisionResult()
        val output = formatter.formatDecisionResult(result)
        assertTrue(output.contains("✅"))
    }

    @Test
    fun `help message contains usage instructions`() {
        val help = formatter.formatHelp()
        assertTrue(help.contains("milk, bread"))
        assertTrue(help.contains("alert"))
        assertTrue(help.contains("location"))
    }

    @Test
    fun `error message contains error indicator`() {
        val error = formatter.formatError("Something went wrong")
        assertTrue(error.startsWith("❌"))
    }

    @Test
    fun `alert set message includes item and price`() {
        val msg = formatter.formatAlertSet("milk", BigDecimal("55.00"))
        assertTrue(msg.contains("milk"))
        assertTrue(msg.contains("55"))
        assertTrue(msg.contains("✅"))
    }
}
