package com.cartiq.assistant.client

import com.cartiq.assistant.model.Platform
import com.cartiq.assistant.model.PriceResult
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

@Component
class InstamartClient(
    @Value("\${platforms.instamart.base-url:https://api.swiggy.com/instamart}") private val baseUrl: String,
    @Value("\${platforms.instamart.api-key:mock}") private val apiKey: String
) : PlatformClient {

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer $apiKey")
        .build()

    override fun search(query: String, location: String): List<PriceResult> {
        return try {
            log.debug { "Instamart search: query=$query location=$location" }
            mockResults(query)
        } catch (e: Exception) {
            log.warn(e) { "Instamart search failed for '$query'" }
            emptyList()
        }
    }

    private fun mockResults(query: String): List<PriceResult> {
        val lowerQuery = query.lowercase()
        return when {
            "milk" in lowerQuery -> listOf(
                PriceResult(Platform.INSTAMART, "Amul Gold Milk 500 ml", "Amul", BigDecimal("33.00"), "500ml",
                    deepLink = "https://swiggy.com/instamart/item/amul-gold-milk/123"),
                PriceResult(Platform.INSTAMART, "Heritage Toned Milk 500ml", "Heritage", BigDecimal("30.00"), "500ml",
                    deepLink = "https://swiggy.com/instamart/item/heritage-milk/124")
            )
            "bread" in lowerQuery -> listOf(
                PriceResult(Platform.INSTAMART, "Britannia Bread Sliced 400g", "Britannia", BigDecimal("40.00"), "400g",
                    deepLink = "https://swiggy.com/instamart/item/britannia-bread/200"),
                PriceResult(Platform.INSTAMART, "Modern Bread Sandwich 400g", "Modern", BigDecimal("37.00"), "400g",
                    deepLink = "https://swiggy.com/instamart/item/modern-bread/201")
            )
            "egg" in lowerQuery || "eggs" in lowerQuery -> listOf(
                PriceResult(Platform.INSTAMART, "Country Eggs 6pcs", "", BigDecimal("58.00"), "6pcs",
                    deepLink = "https://swiggy.com/instamart/item/country-eggs/300")
            )
            "rice" in lowerQuery -> listOf(
                PriceResult(Platform.INSTAMART, "India Gate Classic Basmati Rice 1kg", "India Gate", BigDecimal("148.00"), "1kg",
                    deepLink = "https://swiggy.com/instamart/item/india-gate-basmati/400")
            )
            "banana" in lowerQuery -> listOf(
                PriceResult(Platform.INSTAMART, "Banana (Robusta) 6pcs", "", BigDecimal("50.00"), "6pcs",
                    deepLink = "https://swiggy.com/instamart/item/banana/500")
            )
            else -> {
                val deterministicPrice = 55 + (Math.abs(query.hashCode() * 17) % 156)
                listOf(
                    PriceResult(Platform.INSTAMART, query.replaceFirstChar { it.uppercase() }, "", BigDecimal("$deterministicPrice.00"), "1unit",
                        deepLink = "https://swiggy.com/instamart/search?q=${query.replace(" ", "+")}")
                )
            }
        }
    }
}
