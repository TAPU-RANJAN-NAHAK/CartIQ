package com.cartiq.assistant.client

import com.cartiq.assistant.model.Platform
import com.cartiq.assistant.model.PriceResult
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.math.BigDecimal
import java.time.Duration

private val log = KotlinLogging.logger {}

/**
 * Blinkit platform client.
 *
 * In production this would call Blinkit's internal/partner API.
 * The mock returns realistic-looking data so the entire pipeline
 * can be exercised end-to-end without credentials.
 */
@Component
class BlinkitClient(
    @Value("\${platforms.blinkit.base-url:https://api.blinkit.com}") private val baseUrl: String,
    @Value("\${platforms.blinkit.api-key:mock}") private val apiKey: String
) : PlatformClient {

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer $apiKey")
        .build()

    override fun search(query: String, location: String): List<PriceResult> {
        return try {
            // In a real implementation, call the actual Blinkit API:
            // webClient.get()
            //     .uri("/v1/products/search?q={q}&lat={lat}", query, location)
            //     .retrieve()
            //     .bodyToMono<BlinkitSearchResponse>()
            //     .timeout(Duration.ofSeconds(5))
            //     .block()
            //     ?.products?.map { it.toPriceResult() } ?: emptyList()

            // Mock implementation
            log.debug { "Blinkit search: query=$query location=$location" }
            mockResults(query)
        } catch (e: Exception) {
            log.warn(e) { "Blinkit search failed for '$query'" }
            emptyList()
        }
    }

    private fun mockResults(query: String): List<PriceResult> {
        val lowerQuery = query.lowercase()
        return when {
            "milk" in lowerQuery -> listOf(
                PriceResult(Platform.BLINKIT, "Amul Gold Milk 500ml", "Amul", BigDecimal("32.00"), "500ml",
                    deepLink = "https://blinkit.com/prn/amul-gold-milk/prid/123"),
                PriceResult(Platform.BLINKIT, "Mother Dairy Toned Milk 500ml", "Mother Dairy", BigDecimal("28.00"), "500ml",
                    deepLink = "https://blinkit.com/prn/mother-dairy-milk/prid/124")
            )
            "bread" in lowerQuery -> listOf(
                PriceResult(Platform.BLINKIT, "Britannia Bread 400g", "Britannia", BigDecimal("42.00"), "400g",
                    deepLink = "https://blinkit.com/prn/britannia-bread/prid/200"),
                PriceResult(Platform.BLINKIT, "Modern Bread 400g", "Modern", BigDecimal("38.00"), "400g",
                    deepLink = "https://blinkit.com/prn/modern-bread/prid/201")
            )
            "egg" in lowerQuery || "eggs" in lowerQuery -> listOf(
                PriceResult(Platform.BLINKIT, "Farm Fresh Eggs 6pcs", "Farm Fresh", BigDecimal("55.00"), "6pcs",
                    deepLink = "https://blinkit.com/prn/farm-eggs/prid/300")
            )
            "rice" in lowerQuery -> listOf(
                PriceResult(Platform.BLINKIT, "India Gate Basmati Rice 1kg", "India Gate", BigDecimal("145.00"), "1kg",
                    deepLink = "https://blinkit.com/prn/india-gate-rice/prid/400")
            )
            "banana" in lowerQuery -> listOf(
                PriceResult(Platform.BLINKIT, "Banana 6pcs", "", BigDecimal("48.00"), "6pcs",
                    deepLink = "https://blinkit.com/prn/banana/prid/500")
            )
            else -> {
                val deterministicPrice = 50 + (Math.abs(query.hashCode()) % 151)
                listOf(
                    PriceResult(Platform.BLINKIT, query.replaceFirstChar { it.uppercase() }, "", BigDecimal("$deterministicPrice.00"), "1unit",
                        deepLink = "https://blinkit.com/s?q=${query.replace(" ", "+")}")
                )
            }
        }
    }
}
