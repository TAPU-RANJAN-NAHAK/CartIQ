package com.cartiq.assistant.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {}

@Service
class WhatsAppService(
    @Value("\${whatsapp.api.base-url:https://graph.facebook.com/v19.0}") private val baseUrl: String,
    @Value("\${whatsapp.api.phone-number-id}") private val phoneNumberId: String,
    @Value("\${whatsapp.api.access-token}") private val accessToken: String,
    private val objectMapper: ObjectMapper
) {
    private val webClient: WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer $accessToken")
        .build()

    fun sendMessage(to: String, text: String) {
        val body = mapOf(
            "messaging_product" to "whatsapp",
            "to" to to,
            "type" to "text",
            "text" to mapOf("preview_url" to false, "body" to text)
        )

        webClient.post()
            .uri("/$phoneNumberId/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String::class.java)
            .doOnSuccess { log.debug { "Message sent to $to" } }
            .doOnError { e -> log.error(e) { "Failed to send message to $to" } }
            .onErrorResume { Mono.empty() }
            .subscribe()
    }
}
