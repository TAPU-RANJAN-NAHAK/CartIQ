package com.cartiq.assistant.engine

import com.cartiq.assistant.model.*
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class DecisionEngine(
    private val normalizationEngine: NormalizationEngine
) {
    fun decide(itemResults: Map<String, List<PriceResult>>): DecisionResult {
        val comparisons = itemResults.map { (query, results) ->
            val ranked = normalizationEngine.rankByRelevance(query, results)
            ItemComparison(
                query = query,
                results = ranked,
                cheapest = ranked.minByOrNull { it.price }
            )
        }

        val splitCart = buildSplitCart(comparisons)
        val bestSingleCart = buildBestSingleCart(comparisons)

        return DecisionResult(
            perItemComparisons = comparisons,
            bestSingleCart = bestSingleCart,
            splitCartPlan = splitCart
        )
    }

    private fun buildSplitCart(comparisons: List<ItemComparison>): CartPlan {
        val items = mutableMapOf<String, PriceResult>()
        for (comparison in comparisons) {
            val cheapest = comparison.cheapest ?: continue
            items[comparison.query] = cheapest
        }
        val total = items.values.sumOf { it.price }
        val platformLinks = items.values
            .groupBy { it.platform }
            .mapValues { (_, results) -> results.first().deepLink }

        return CartPlan(CartPlanType.SPLIT_CART, items, total, platformLinks)
    }

    private fun buildBestSingleCart(comparisons: List<ItemComparison>): CartPlan? {
        // Group all results by platform, pick cheapest per item per platform, sum up
        val platforms = Platform.values()
        val platformTotals = platforms.mapNotNull { platform ->
            val itemsOnPlatform = mutableMapOf<String, PriceResult>()
            for (comparison in comparisons) {
                val result = comparison.results.filter { it.platform == platform }.minByOrNull { it.price }
                    ?: return@mapNotNull null  // platform doesn't have this item -> skip
                itemsOnPlatform[comparison.query] = result
            }
            val total = itemsOnPlatform.values.sumOf { it.price }
            val links = mapOf(platform to (itemsOnPlatform.values.firstOrNull()?.deepLink ?: ""))
            CartPlan(CartPlanType.SINGLE_APP, itemsOnPlatform, total, links)
        }
        return platformTotals.minByOrNull { it.total }
    }
}
