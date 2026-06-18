package com.cartiq.assistant.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}

/**
 * Validates the `X-Hub-Signature-256` header on every POST to `/webhook`.
 *
 * Meta signs each webhook delivery with HMAC-SHA256 using the app secret.
 * The header value has the form `sha256=<hex-digest>`.
 *
 * When [appSecret] is blank (e.g. local development) validation is skipped entirely.
 */
@Component
class WebhookSignatureFilter(
    @Value("\${whatsapp.webhook.app-secret:}") private val appSecret: String
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Only validate POST requests to /webhook; let everything else through.
        if (request.method != "POST" || !request.requestURI.startsWith("/webhook")) {
            filterChain.doFilter(request, response)
            return
        }

        // If no app secret is configured, skip validation (dev mode).
        if (appSecret.isBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        // Wrap the request so the body can be read twice (once here, once in the controller).
        val cachingRequest = ContentCachingRequestWrapper(request)

        val signatureHeader = cachingRequest.getHeader("X-Hub-Signature-256")
        if (signatureHeader.isNullOrBlank()) {
            log.warn { "Missing X-Hub-Signature-256 header on webhook POST — rejecting" }
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing signature")
            return
        }

        // Read the full body to compute the HMAC (the wrapper caches it for downstream).
        filterChain.doFilter(cachingRequest, response)
        val body = cachingRequest.contentAsByteArray

        val expectedHex = computeHmacSha256(body, appSecret)
        val expectedHeader = "sha256=$expectedHex"

        if (!signatureHeader.equals(expectedHeader, ignoreCase = true)) {
            log.warn { "Webhook signature mismatch — possible spoofed request" }
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature")
        }
    }

    private fun computeHmacSha256(data: ByteArray, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }
}
