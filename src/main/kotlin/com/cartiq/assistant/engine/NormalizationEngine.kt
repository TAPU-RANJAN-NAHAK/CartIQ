package com.cartiq.assistant.engine

import org.apache.commons.text.similarity.LevenshteinDistance
import org.springframework.stereotype.Component

@Component
class NormalizationEngine {

    private val levenshtein = LevenshteinDistance()

    // Known brand tokens for detection
    private val knownBrands = setOf(
        "amul", "britannia", "modern", "harvest gold", "mother dairy",
        "nandini", "heritage", "india gate", "daawat", "farm fresh"
    )

    /**
     * Parse a comma-separated or newline-separated user message into individual item queries.
     */
    fun parseItems(message: String): List<String> {
        return message
            .replace(Regex("\\band\\b", RegexOption.IGNORE_CASE), ",")
            .replace(";", ",")
            .replace("\n", ",")
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /**
     * Tokenize a product name into normalized keywords.
     */
    fun tokenize(name: String): Set<String> {
        return name.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(" ")
            .filter { it.length > 1 }
            .toSet()
    }

    /**
     * Detect a brand from a product name.
     */
    fun detectBrand(name: String): String? {
        val lower = name.lowercase()
        return knownBrands.firstOrNull { lower.contains(it) }
    }

    /**
     * Compute normalized similarity score between query and product name.
     * Returns a value in [0.0, 1.0] — higher is better.
     */
    fun similarity(query: String, productName: String): Double {
        val queryTokens = tokenize(query)
        val nameTokens = tokenize(productName)
        val intersection = queryTokens.intersect(nameTokens).size
        val union = queryTokens.union(nameTokens).size.toDouble()
        val jaccard = if (union == 0.0) 0.0 else intersection / union

        // Also compute Levenshtein-based similarity as a fallback
        val maxLen = maxOf(query.length, productName.length).toDouble()
        val levScore = if (maxLen == 0.0) 1.0 else {
            1.0 - (levenshtein.apply(query.lowercase(), productName.lowercase()) / maxLen)
        }

        return (jaccard * 0.7) + (levScore * 0.3)
    }

    /**
     * Filter and rank results by relevance to the user query.
     */
    fun rankByRelevance(query: String, results: List<com.cartiq.assistant.model.PriceResult>): List<com.cartiq.assistant.model.PriceResult> {
        val threshold = 0.15
        return results
            .map { it to similarity(query, it.productName) }
            .filter { (_, score) -> score >= threshold }
            .sortedByDescending { (_, score) -> score }
            .map { (result, _) -> result }
    }
}
