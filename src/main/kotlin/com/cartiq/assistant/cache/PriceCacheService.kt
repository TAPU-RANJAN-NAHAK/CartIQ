package com.cartiq.assistant.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.cartiq.assistant.model.PriceResult
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

private val log = KotlinLogging.logger {}

@Service
class PriceCacheService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val ttl: Duration = Duration.ofMinutes(7)

    private fun key(query: String, location: String) =
        "price:${query.lowercase().trim()}:${location.lowercase().trim()}"

    fun get(query: String, location: String): List<PriceResult>? {
        return try {
            val json = redisTemplate.opsForValue().get(key(query, location)) ?: return null
            objectMapper.readValue<List<PriceResult>>(json)
        } catch (e: Exception) {
            log.warn(e) { "Cache read failed for '$query'" }
            null
        }
    }

    fun put(query: String, location: String, results: List<PriceResult>) {
        try {
            val json = objectMapper.writeValueAsString(results)
            redisTemplate.opsForValue().set(key(query, location), json, ttl)
        } catch (e: Exception) {
            log.warn(e) { "Cache write failed for '$query'" }
        }
    }

    fun evict(query: String, location: String) {
        try {
            redisTemplate.delete(key(query, location))
        } catch (e: Exception) {
            log.warn(e) { "Cache eviction failed for '$query'" }
        }
    }
}
