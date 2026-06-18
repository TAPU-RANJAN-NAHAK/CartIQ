package com.cartiq.assistant.controller

import com.cartiq.assistant.service.OrchestratorService
import com.cartiq.assistant.service.WhatsAppService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/webhook")
class WhatsAppWebhookController(
    private val orchestratorService: OrchestratorService,
    private val whatsAppService: WhatsAppService,
    @Value("\${whatsapp.webhook.verify-token}") private val verifyToken: String
) {
    /**
     * Meta webhook verification (GET).
     */
    @GetMapping
    fun verify(
        @RequestParam("hub.mode") mode: String,
        @RequestParam("hub.verify_token") token: String,
        @RequestParam("hub.challenge") challenge: String
    ): ResponseEntity<String> {
        return if (mode == "subscribe" && token == verifyToken) {
            log.info { "Webhook verified" }
            ResponseEntity.ok(challenge)
        } else {
            log.warn { "Webhook verification failed: mode=$mode token=$token" }
            ResponseEntity.status(403).body("Forbidden")
        }
    }

    /**
     * Receive incoming WhatsApp messages (POST).
     */
    @PostMapping
    fun receive(@RequestBody payload: Map<String, Any>): ResponseEntity<String> {
        try {
            val (phone, text) = extractMessage(payload) ?: return ResponseEntity.ok("ok")
            log.info { "Received message from $phone: $text" }

            val response = orchestratorService.handle(phone, text)
            whatsAppService.sendMessage(phone, response)
        } catch (e: Exception) {
            log.error(e) { "Error processing webhook" }
        }
        return ResponseEntity.ok("ok")
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractMessage(payload: Map<String, Any>): Pair<String, String>? {
        return try {
            val entry = (payload["entry"] as? List<*>)?.firstOrNull() as? Map<String, Any> ?: return null
            val changes = (entry["changes"] as? List<*>)?.firstOrNull() as? Map<String, Any> ?: return null
            val value = changes["value"] as? Map<String, Any> ?: return null
            val messages = value["messages"] as? List<*> ?: return null
            val message = messages.firstOrNull() as? Map<String, Any> ?: return null

            val from = message["from"] as? String ?: return null
            val textMap = message["text"] as? Map<String, Any> ?: return null
            val body = textMap["body"] as? String ?: return null

            Pair(from, body)
        } catch (e: Exception) {
            log.warn(e) { "Failed to parse webhook payload" }
            null
        }
    }
}
