package com.cartiq.assistant.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthController {
    @GetMapping("/")
    fun root() = mapOf("status" to "ok", "service" to "QuickCommerce Price Assistant", "ts" to Instant.now())
}
