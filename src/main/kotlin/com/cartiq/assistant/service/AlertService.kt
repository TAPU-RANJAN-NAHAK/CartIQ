package com.cartiq.assistant.service

import com.cartiq.assistant.aggregator.PriceAggregator
import com.cartiq.assistant.cache.PriceCacheService
import com.cartiq.assistant.model.Alert
import com.cartiq.assistant.model.AlertRepository
import com.cartiq.assistant.model.User
import com.cartiq.assistant.model.UserRepository
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

@Service
class AlertService(
    private val alertRepository: AlertRepository,
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val priceAggregator: PriceAggregator,
    private val cacheService: PriceCacheService,
    private val whatsAppService: WhatsAppService
) {
    @Transactional
    fun createAlert(phone: String, item: String, targetPrice: BigDecimal): Alert {
        val user = userService.getOrCreate(phone)
        return alertRepository.save(
            Alert(userId = user.id, itemQuery = item, targetPrice = targetPrice)
        )
    }

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    fun checkAlerts() {
        val activeAlerts = alertRepository.findByActive(true)
        if (activeAlerts.isEmpty()) return
        log.info { "Checking ${activeAlerts.size} active alerts" }

        activeAlerts.forEach { alert ->
            val user = userRepository.findById(alert.userId).orElse(null) ?: return@forEach
            checkSingleAlert(alert, user)
        }
    }

    private fun checkSingleAlert(alert: Alert, user: User) {
        val location = user.location.ifBlank { "Delhi" }
        val results = runBlocking {
            val cached = cacheService.get(alert.itemQuery, location)
            cached ?: priceAggregator.fetchAll(alert.itemQuery, location).also {
                cacheService.put(alert.itemQuery, location, it)
            }
        }
        val cheapest = results.minByOrNull { it.price } ?: return
        if (cheapest.price <= alert.targetPrice) {
            log.info { "Alert triggered: ${alert.itemQuery} at ₹${cheapest.price} on ${cheapest.platform}" }
            val msg = """
                🔔 *Price Alert!*
                ${alert.itemQuery.replaceFirstChar { it.uppercase() }} is now ₹${cheapest.price} on ${cheapest.platform.name} ✅
                (Your target: ₹${alert.targetPrice})
                👉 ${cheapest.deepLink}
            """.trimIndent()
            whatsAppService.sendMessage(user.phone, msg)
            alertRepository.save(alert.copy(active = false))
        }
    }
}
