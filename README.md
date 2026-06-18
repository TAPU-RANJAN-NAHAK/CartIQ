# QuickCommerce Smart Price Assistant

A WhatsApp-first grocery price comparison bot that aggregates prices from **Blinkit**, **Zepto**, and **Instamart** in real-time, helping users find the cheapest options with zero friction.

## Architecture

```
User (WhatsApp)
       ↓
WhatsApp Cloud API (Meta)
       ↓
Webhook Controller (Spring Boot)
       ↓
Orchestrator Service
       ↓
┌─────────────────────────────────┐
│    Price Aggregation Layer      │
│  (BlinkitClient | ZeptoClient   │
│   | InstamartClient)  ← parallel│
└─────────────────────────────────┘
       ↓
Normalization + Matching Engine
       ↓
Decision Engine
       ↓
Redis Cache (7-min TTL)
       ↓
Response Formatter (WhatsApp UX)
       ↓
WhatsApp API → User
```

## Features

| Feature | Description |
|---|---|
| 🔍 Price comparison | Compare prices across Blinkit, Zepto, Instamart instantly |
| 🛒 Split cart optimization | Pick cheapest platform per item |
| 📱 Single app cart | Best platform if you want one app |
| 🔔 Price alerts | Notify when item drops below target |
| 📍 Location-aware | Set city for relevant prices |
| ⚡ Redis caching | 7-min cache to avoid rate limits |
| 🧠 Fuzzy matching | Jaccard + Levenshtein to handle naming variations |
| 🔄 Async fetching | Kotlin coroutines for parallel platform calls |

## Tech Stack

- **Backend**: Spring Boot 3 + Kotlin
- **Coroutines**: `kotlinx-coroutines-core` for parallel platform calls
- **Cache**: Redis (Lettuce)
- **DB**: PostgreSQL (JPA/Hibernate)
- **HTTP**: WebFlux WebClient
- **Matching**: Apache Commons Text (Levenshtein)
- **Messaging**: Meta WhatsApp Cloud API

## Quick Start

### 1. Set environment variables

```bash
export WHATSAPP_PHONE_NUMBER_ID=<your-phone-number-id>
export WHATSAPP_ACCESS_TOKEN=<your-access-token>
export WHATSAPP_VERIFY_TOKEN=your-webhook-verify-token
```

### 2. Run with Docker Compose

```bash
docker-compose up --build
```

### 3. Expose webhook (ngrok for local dev)

```bash
ngrok http 8080
# Set webhook URL in Meta Developer Console: https://<ngrok-url>/webhook
```

## Bot Commands

| Message | Action |
|---|---|
| `milk, bread, eggs` | Compare prices for listed items |
| `location Mumbai` | Set your delivery location |
| `alert milk < 60` | Alert when milk drops below ₹60 |
| `help` | Show usage guide |

## Sample WhatsApp Response

```
🔍 *Price Comparison*

📦 *Milk*
  🟡 Blinkit: ₹32.00
  🟣 Zepto: ₹27.00 ✅
  🟠 Instamart: ₹33.00

📦 *Bread*
  🟡 Blinkit: ₹38.00 ✅
  🟣 Zepto: ₹44.00
  🟠 Instamart: ₹40.00

─────────────────────
🛒 *Best Split Cart (Cheapest Overall)*
  • Milk → ZEPTO ₹27.00 ✅
  • Bread → BLINKIT ₹38.00 ✅

💰 *Total: ₹65.00*

👉 *Open Apps:*
  🟣 [ZEPTO] https://www.zeptonow.com/...
  🟡 [BLINKIT] https://blinkit.com/...
```

## Project Structure

```
src/main/kotlin/com/quickcommerce/assistant/
├── QuickCommerceApplication.kt     # Entry point
├── config/
│   └── AppConfig.kt                # ObjectMapper, Redis, Scheduling
├── controller/
│   ├── WhatsAppWebhookController.kt # GET (verify) + POST (receive messages)
│   └── HealthController.kt
├── service/
│   ├── OrchestratorService.kt      # Brain: routes messages, coordinates pipeline
│   ├── UserService.kt              # User management
│   ├── AlertService.kt             # Price alerts + scheduled checks
│   └── WhatsAppService.kt          # Send messages via Meta API
├── aggregator/
│   └── PriceAggregator.kt          # Parallel coroutine-based fetching
├── client/
│   ├── PlatformClient.kt           # Interface (Adapter pattern)
│   ├── BlinkitClient.kt
│   ├── ZeptoClient.kt
│   └── InstamartClient.kt
├── engine/
│   ├── NormalizationEngine.kt      # Tokenization, Levenshtein, Jaccard
│   └── DecisionEngine.kt           # Split cart + single app cart logic
├── cache/
│   └── PriceCacheService.kt        # Redis get/put/evict with 7-min TTL
├── formatter/
│   └── ResponseFormatter.kt        # WhatsApp-friendly text output
└── model/
    ├── Models.kt                   # JPA entities + in-memory DTOs
    └── Repositories.kt             # Spring Data JPA repositories
```

## Adding a New Platform

1. Create `client/MyPlatformClient.kt` implementing `PlatformClient`
2. Add it to the Spring context (`@Component`)
3. Add `@Value` config keys in `application.yml`
4. The `PriceAggregator` auto-discovers all `PlatformClient` beans

## Running Tests

```bash
./gradlew test
```

## Scaling Roadmap

| Phase | Users | Changes |
|---|---|---|
| MVP | 0–100 | Single server, mock platform adapters |
| V2 | 1K | Redis mandatory, real platform APIs |
| V3 | 10K+ | Microservices split, proxy rotation |
| V4 | 100K+ | Event-driven (Kafka), ML-based matching |
