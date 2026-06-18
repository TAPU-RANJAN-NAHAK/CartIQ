package com.cartiq.assistant.aggregator

import com.cartiq.assistant.client.PlatformClient
import com.cartiq.assistant.model.PriceResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class PriceAggregator(
    private val platformClients: List<PlatformClient>
) {
    /**
     * Fetch prices for a single item from ALL platforms in parallel.
     */
    suspend fun fetchAll(query: String, location: String): List<PriceResult> = coroutineScope {
        val deferred = platformClients.map { client ->
            async {
                try {
                    client.search(query, location)
                } catch (e: Exception) {
                    log.warn(e) { "Platform client failed for '$query'" }
                    emptyList()
                }
            }
        }
        deferred.flatMap { it.await() }
    }

    /**
     * Fetch prices for multiple items across all platforms, all in parallel.
     */
    suspend fun fetchAllItems(queries: List<String>, location: String): Map<String, List<PriceResult>> =
        coroutineScope {
            val deferred = queries.map { query ->
                query to async { fetchAll(query, location) }
            }
            deferred.associate { (query, d) -> query to d.await() }
        }
}
