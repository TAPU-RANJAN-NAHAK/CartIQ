package com.cartiq.assistant.client

import com.cartiq.assistant.model.PriceResult

interface PlatformClient {
    fun search(query: String, location: String): List<PriceResult>
}
