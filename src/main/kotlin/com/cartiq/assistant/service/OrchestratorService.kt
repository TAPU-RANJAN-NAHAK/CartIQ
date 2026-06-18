package com.cartiq.assistant.service

import com.cartiq.assistant.aggregator.PriceAggregator
import com.cartiq.assistant.cache.PriceCacheService
import com.cartiq.assistant.engine.DecisionEngine
import com.cartiq.assistant.engine.NormalizationEngine
import com.cartiq.assistant.formatter.ResponseFormatter
import com.cartiq.assistant.model.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class OrchestratorService(
    private val normalizationEngine: NormalizationEngine,
    private val priceAggregator: PriceAggregator,
    private val cacheService: PriceCacheService,
    private val decisionEngine: DecisionEngine,
    private val responseFormatter: ResponseFormatter,
    private val userService: UserService,
    private val alertService: AlertService,
    private val priceSnapshotRepository: PriceSnapshotRepository
) {
    fun handle(phone: String, message: String): String {
        val trimmed = message.trim()
        val lower = trimmed.lowercase()

        // Handle location command
        if (lower.startsWith("location ")) {
            val location = trimmed.substringAfter("location ").trim()
            userService.setLocation(phone, location)
            return responseFormatter.formatLocationSet(location)
        }

        // Handle alert command: "alert milk < 60"
        if (lower.startsWith("alert ")) {
            return handleAlert(phone, trimmed)
        }

        // Handle help
        if (lower == "help" || lower == "hi" || lower == "hello") {
            return responseFormatter.formatHelp()
        }

        // Default: treat as grocery list
        return handlePriceQuery(phone, trimmed)
    }

    private fun handlePriceQuery(phone: String, message: String): String {
        val user = userService.getOrCreate(phone)
        val location = user.location.ifBlank { "Delhi" }
        val items = normalizationEngine.parseItems(message)

        if (items.isEmpty()) {
            return responseFormatter.formatError("I couldn't understand your list")
        }

        log.info { "Price query: phone=$phone items=$items location=$location" }

        // Separate cached items from those needing a fresh fetch
        val cachedResults = mutableMapOf<String, List<PriceResult>>()
        val uncachedItems = mutableListOf<String>()
        for (item in items) {
            val cached = cacheService.get(item, location)
            if (cached != null) {
                log.debug { "Cache hit for '$item'" }
                cachedResults[item] = cached
            } else {
                uncachedItems.add(item)
            }
        }

        // Fetch all uncached items in parallel via fetchAllItems()
        val freshResults: Map<String, List<PriceResult>> = if (uncachedItems.isNotEmpty()) {
            runBlocking { priceAggregator.fetchAllItems(uncachedItems, location) }
        } else {
            emptyMap()
        }

        // Persist fresh results as PriceSnapshots and warm the cache
        for ((item, results) in freshResults) {
            cacheService.put(item, location, results)
            persistSnapshots(item, results)
        }

        val itemResults = cachedResults + freshResults
        val decision = decisionEngine.decide(itemResults)
        return responseFormatter.formatDecisionResult(decision)
    }

    /**
     * Persist a batch of PriceResult records as PriceSnapshot rows.
     * productId is a stable hex identifier derived from the normalised query string,
     * avoiding the need for a full Product catalogue at this stage.
     */
    private fun persistSnapshots(query: String, results: List<PriceResult>) {
        if (results.isEmpty()) return
        val productId = Integer.toHexString(query.lowercase().trim().hashCode())
        val snapshots = results.map { r ->
            PriceSnapshot(
                productId = productId,
                platform = r.platform,
                price = r.price,
                unit = r.unit,
                imageUrl = r.imageUrl,
                deepLink = r.deepLink,
                available = r.available
            )
        }
        try {
            priceSnapshotRepository.saveAll(snapshots)
            log.debug { "Persisted ${snapshots.size} price snapshots for '$query'" }
        } catch (e: Exception) {
            log.warn(e) { "Failed to persist price snapshots for '$query'" }
        }
    }

    private fun handleAlert(phone: String, message: String): String {
        // Pattern: alert <item> < <price>
        val regex = Regex("""alert (.+?)\s*[<]\s*([\d.]+)""", RegexOption.IGNORE_CASE)
        val match = regex.find(message) ?: return responseFormatter.formatError("Invalid alert format. Use: alert milk < 60")
        val item = match.groupValues[1].trim()
        val price = match.groupValues[2].toBigDecimalOrNull()
            ?: return responseFormatter.formatError("Invalid price in alert")

        alertService.createAlert(phone, item, price)
        return responseFormatter.formatAlertSet(item, price)
    }
}
