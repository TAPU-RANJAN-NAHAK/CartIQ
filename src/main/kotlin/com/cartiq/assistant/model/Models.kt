package com.cartiq.assistant.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

enum class Platform {
    BLINKIT, ZEPTO, INSTAMART
}

@Entity
@Table(name = "users")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",
    @Column(unique = true, nullable = false)
    val phone: String,
    val location: String = "",
    val preferredBrands: String = "", // comma-separated
    val budget: BigDecimal? = null,
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "products")
data class Product(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",
    val name: String,
    val brand: String = "",
    val category: String = ""
)

@Entity
@Table(name = "price_snapshots")
data class PriceSnapshot(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",
    val productId: String,
    @Enumerated(EnumType.STRING)
    val platform: Platform,
    val price: BigDecimal,
    val unit: String = "",
    val imageUrl: String = "",
    val deepLink: String = "",
    val available: Boolean = true,
    val timestamp: Instant = Instant.now()
)

@Entity
@Table(name = "alerts")
data class Alert(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",
    val userId: String,
    val itemQuery: String,
    val targetPrice: BigDecimal,
    val active: Boolean = true,
    val createdAt: Instant = Instant.now()
)

// DTO used in-memory across the pipeline
data class PriceResult(
    val platform: Platform,
    val productName: String,
    val brand: String = "",
    val price: BigDecimal,
    val unit: String = "",
    val imageUrl: String = "",
    val deepLink: String = "",
    val available: Boolean = true
)

data class ItemComparison(
    val query: String,
    val results: List<PriceResult>,
    val cheapest: PriceResult?
)

data class CartPlan(
    val type: CartPlanType,
    val items: Map<String, PriceResult>, // query -> chosen result
    val total: BigDecimal,
    val platformLinks: Map<Platform, String>
)

enum class CartPlanType {
    SINGLE_APP, SPLIT_CART
}

data class DecisionResult(
    val perItemComparisons: List<ItemComparison>,
    val bestSingleCart: CartPlan?,
    val splitCartPlan: CartPlan
)

// WhatsApp message models
data class WhatsAppMessage(
    val from: String,
    val body: String,
    val messageId: String
)

data class WhatsAppWebhookPayload(
    val `object`: String,
    val entry: List<Map<String, Any>>
)
