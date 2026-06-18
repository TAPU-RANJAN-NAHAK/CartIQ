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
class ZeptoClient(
    @Value("\${platforms.zepto.base-url:https://api.zepto.com}") private val baseUrl: String,
    @Value("\${platforms.zepto.api-key:mock}") private val apiKey: String
) : PlatformClient {

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("x-api-key", apiKey)
        .build()

    override fun search(query: String, location: String): List<PriceResult> {
        return try {
            log.debug { "Zepto search: query=$query location=$location" }
            mockResults(query)
        } catch (e: Exception) {
            log.warn(e) { "Zepto search failed for '$query'" }
            emptyList()
        }
    }

    private fun mockResults(query: String): List<PriceResult> {
        val lowerQuery = query.lowercase()
        return when {
            "milk" in lowerQuery -> listOf(
                PriceResult(Platform.ZEPTO, "Amul Gold Full Cream Milk 500ml", "Amul", BigDecimal("31.00"), "500ml",
                    deepLink = "https://www.zeptonow.com/product/amul-gold-milk/123"),
                PriceResult(Platform.ZEPTO, "Nandini Toned Milk 500ml", "Nandini", BigDecimal("27.00"), "500ml",
                    deepLink = "https://www.zeptonow.com/product/nandini-milk/124")
            )
            "bread" in lowerQuery -> listOf(
                PriceResult(Platform.ZEPTO, "Britannia 100% Whole Wheat Bread", "Britannia", BigDecimal("44.00"), "400g",
                    deepLink = "https://www.zeptonow.com/product/britannia-wheat-bread/200"),
                PriceResult(Platform.ZEPTO, "Harvest Gold Bread", "Harvest Gold", BigDecimal("36.00"), "400g",
                    deepLink = "https://www.zeptonow.com/product/harvest-gold-bread/201")
            )
            "egg" in lowerQuery || "eggs" in lowerQuery -> listOf(
                PriceResult(Platform.ZEPTO, "Eggs White 6pcs", "", BigDecimal("52.00"), "6pcs",
                    deepLink = "https://www.zeptonow.com/product/eggs-white/300")
            )
            "rice" in lowerQuery -> listOf(
                PriceResult(Platform.ZEPTO, "India Gate Basmati 1kg", "India Gate", BigDecimal("140.00"), "1kg",
                    deepLink = "https://www.zeptonow.com/product/india-gate-basmati/400"),
                PriceResult(Platform.ZEPTO, "Daawat Rozana Basmati 1kg", "Daawat", BigDecimal("130.00"), "1kg",
                    deepLink = "https://www.zeptonow.com/product/daawat-basmati/401")
            )
            "banana" in lowerQuery -> listOf(
                PriceResult(Platform.ZEPTO, "Fresh Banana 1 dozen", "", BigDecimal("45.00"), "12pcs",
                    deepLink = "https://www.zeptonow.com/product/banana/500")
            )
            else -> {
                val deterministicPrice = 45 + (Math.abs(query.hashCode() * 31) % 146)
                listOf(
                    PriceResult(Platform.ZEPTO, query.replaceFirstChar { it.uppercase() }, "", BigDecimal("$deterministicPrice.00"), "1unit",
                        deepLink = "https://www.zeptonow.com/search?q=${query.replace(" ", "+")}")
                )
            }
        }
    }
}
