package com.cartiq.assistant.formatter

import com.cartiq.assistant.model.*
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ResponseFormatter {

    fun formatDecisionResult(result: DecisionResult): String {
        val sb = StringBuilder()

        // Per-item cheapest
        sb.appendLine("🔍 *Price Comparison*")
        sb.appendLine()
        for (comparison in result.perItemComparisons) {
            sb.appendLine("📦 *${comparison.query.replaceFirstChar { it.uppercase() }}*")
            if (comparison.results.isEmpty()) {
                sb.appendLine("  ❌ Not found on any platform")
            } else {
                val top = comparison.results.take(3)
                for (r in top) {
                    val cheapestMark = if (r == comparison.cheapest) " ✅" else ""
                    sb.appendLine("  ${platformEmoji(r.platform)} ${r.platform.name.lowercase().replaceFirstChar { it.uppercase() }}: ₹${r.price}$cheapestMark")
                }
            }
            sb.appendLine()
        }

        // Split cart recommendation
        sb.appendLine("─────────────────────")
        sb.appendLine("🛒 *Best Split Cart (Cheapest Overall)*")
        for ((query, item) in result.splitCartPlan.items) {
            sb.appendLine("  • ${query.replaceFirstChar { it.uppercase() }} → ${item.platform.name} ₹${item.price} ✅")
        }
        sb.appendLine()
        sb.appendLine("💰 *Total: ₹${result.splitCartPlan.total}*")
        sb.appendLine()

        // Best single-app cart
        if (result.bestSingleCart != null) {
            val single = result.bestSingleCart
            val platform = single.items.values.firstOrNull()?.platform
            sb.appendLine("─────────────────────")
            sb.appendLine("📱 *Best Single App Cart* (${platform?.name ?: ""})")
            for ((query, item) in single.items) {
                sb.appendLine("  • ${query.replaceFirstChar { it.uppercase() }} → ₹${item.price}")
            }
            sb.appendLine("💰 Total: ₹${single.total}")
            val savings = single.total - result.splitCartPlan.total
            if (savings > BigDecimal.ZERO) {
                sb.appendLine("⚠️ Costs ₹$savings more than split cart")
            } else {
                sb.appendLine("✅ Same or better than split cart!")
            }
            sb.appendLine()
        }

        // Deep links
        sb.appendLine("👉 *Open Apps:*")
        val allLinks = result.splitCartPlan.platformLinks
        for ((platform, link) in allLinks) {
            if (link.isNotBlank()) {
                sb.appendLine("  ${platformEmoji(platform)} [${platform.name}] $link")
            }
        }

        return sb.toString().trim()
    }

    fun formatHelp(): String = """
        👋 *CartIQ – Smart Grocery Price Assistant*
        
        I help you find the cheapest groceries across Blinkit, Zepto & Instamart!
        
        *How to use:*
        • Send items: `milk, bread, eggs`
        • Set alert: `alert milk < 60`
        • Set location: `location Mumbai`
        
        I'll instantly compare prices and show you the best deals! 🚀
    """.trimIndent()

    fun formatError(message: String): String = "❌ $message. Please try again or type *help* for usage."

    fun formatAlertSet(item: String, price: BigDecimal): String =
        "✅ Alert set! I'll notify you when *$item* drops below ₹$price."

    fun formatLocationSet(location: String): String =
        "📍 Location set to *$location*. Now send me your grocery list!"

    private fun platformEmoji(platform: Platform): String = when (platform) {
        Platform.BLINKIT -> "🟡"
        Platform.ZEPTO -> "🟣"
        Platform.INSTAMART -> "🟠"
    }
}
