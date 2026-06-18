# CartIQ 🛒

> **Smart grocery price assistant on WhatsApp.**  
> Send your grocery list → CartIQ compares prices across **Blinkit**, **Zepto** and **Instamart** in real time and replies with the cheapest option — per item and as a full cart.

---

## Demo

```
You:     milk, bread, eggs
CartIQ:  🔍 Price Comparison

         📦 Milk
           🟡 Blinkit:   ₹32.00
           🟣 Zepto:     ₹27.00 ✅
           🟠 Instamart: ₹33.00

         📦 Bread
           🟡 Blinkit:   ₹38.00 ✅
           🟣 Zepto:     ₹44.00
           🟠 Instamart: ₹40.00

         ─────────────────────
         🛒 Best Split Cart (Cheapest Overall)
           • Milk  → ZEPTO    ₹27.00 ✅
           • Bread → BLINKIT  ₹38.00 ✅

         💰 Total: ₹65.00

         👉 Open Apps:
           🟣 [ZEPTO]   https://www.zeptonow.com/...
           🟡 [BLINKIT] https://blinkit.com/...
```

---

## Features

| Feature | Details |
|---------|---------|
| 🔍 Price comparison | Live prices from Blinkit, Zepto, Instamart |
| 🛒 Split cart | Cheapest platform per individual item |
| 📱 Single-app cart | Best one-app option with savings comparison |
| 🔔 Price alerts | `alert milk < 60` — notifies when price drops |
| 📍 Location-aware | `location Mumbai` personalises results |
| ⚡ Redis cache | 7-minute TTL to prevent redundant API calls |
| 🧠 Fuzzy matching | Jaccard + Levenshtein for messy grocery names |
| 🔄 Parallel fetch | Kotlin coroutines — all platforms hit simultaneously |
| 🔒 HMAC validation | Meta `X-Hub-Signature-256` webhook signature check |
| 🗄️ Price history | Every fetch persisted to `price_snapshots` in PostgreSQL |

---

## Architecture

```
WhatsApp User
      │
      ▼
Meta Cloud API
      │  POST /webhook  (HMAC-SHA256 validated)
      ▼
WebhookSignatureFilter  ──► 401 if signature invalid
      │
      ▼
WhatsAppWebhookController
      │
      ▼
OrchestratorService  (routes: location / alert / price query)
      │
      ├── NormalizationEngine   (tokenize → Levenshtein/Jaccard match)
      │
      ├── PriceAggregator       (coroutine scope — all platforms in parallel)
      │     ├── BlinkitClient
      │     ├── ZeptoClient
      │     └── InstamartClient
      │
      ├── PriceCacheService     (Redis, 7-min TTL)
      ├── PriceSnapshotRepository  (PostgreSQL — price history)
      │
      ├── DecisionEngine        (split cart + best single-app cart)
      │
      └── ResponseFormatter     (WhatsApp-friendly text + deep links)
            │
            ▼
      WhatsAppService  ──► Meta Cloud API ──► User
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9 |
| Framework | Spring Boot 3.2.5 |
| Async | kotlinx-coroutines |
| Database | PostgreSQL 16 (JPA / Hibernate) |
| Cache | Redis 7 (Lettuce) |
| HTTP Client | WebFlux WebClient |
| NLP Matching | Apache Commons Text (Levenshtein) |
| Messaging | Meta WhatsApp Cloud API |
| Containers | Docker + Docker Compose |

---

## Project Structure

```
src/main/kotlin/com/cartiq/assistant/
├── CartIQApplication.kt
├── config/
│   ├── AppConfig.kt                  # ObjectMapper, Redis, @EnableScheduling
│   └── WebhookSignatureFilter.kt     # HMAC-SHA256 signature validation
├── controller/
│   ├── WhatsAppWebhookController.kt  # GET verify + POST receive
│   └── HealthController.kt
├── service/
│   ├── OrchestratorService.kt        # Message router + pipeline coordinator
│   ├── UserService.kt                # getOrCreate, setLocation
│   ├── AlertService.kt               # createAlert + @Scheduled checker
│   └── WhatsAppService.kt            # Meta Cloud API sender
├── aggregator/
│   └── PriceAggregator.kt            # fetchAll (1 item) + fetchAllItems (N items)
├── client/
│   ├── PlatformClient.kt             # Interface — add new platforms here
│   ├── BlinkitClient.kt
│   ├── ZeptoClient.kt
│   └── InstamartClient.kt
├── engine/
│   ├── NormalizationEngine.kt        # Tokenization + fuzzy matching
│   └── DecisionEngine.kt             # Split cart + single-app logic
├── cache/
│   └── PriceCacheService.kt          # Redis get / put / evict
├── formatter/
│   └── ResponseFormatter.kt          # WhatsApp text formatting
└── model/
    ├── Models.kt                     # JPA entities + in-memory DTOs
    └── Repositories.kt               # Spring Data JPA repositories
```

---

## Getting Started

### Prerequisites

- **Docker Desktop** — [download](https://www.docker.com/products/docker-desktop/)
- **ngrok** (for local webhook testing) — [download](https://ngrok.com/download)
- **Meta WhatsApp Business API credentials** — see [Step 2](#step-2--get-whatsapp-credentials) below

---

### Step 1 — Clone the repo

```bash
git clone https://github.com/TAPU-RANJAN-NAHAK/CartIQ.git
cd CartIQ
```

---

### Step 2 — Get WhatsApp credentials

1. Go to **https://developers.facebook.com** → **My Apps** → **Create App**
2. Choose **Business** type → give it a name (e.g. *CartIQ*)
3. Add the **WhatsApp** product to your app
4. Under **WhatsApp → API Setup**, note:
   - `Phone Number ID` → your `WHATSAPP_PHONE_NUMBER_ID`
   - `Temporary access token` (or create a permanent one via System User) → `WHATSAPP_ACCESS_TOKEN`
5. Under **App Settings → Basic**, copy:
   - `App Secret` → your `WHATSAPP_APP_SECRET`
6. Under **WhatsApp → Configuration**, you'll set the webhook URL in Step 5

---

### Step 3 — Create your `.env` file

```bash
# In the project root — this file is gitignored, never commit it
cat > .env << 'EOF'
WHATSAPP_PHONE_NUMBER_ID=your_phone_number_id_here
WHATSAPP_ACCESS_TOKEN=your_access_token_here
WHATSAPP_VERIFY_TOKEN=cartiq-verify-token
WHATSAPP_APP_SECRET=your_app_secret_here
EOF
```

> **Tip:** Leave `WHATSAPP_APP_SECRET` blank while testing locally to skip signature validation.

---

### Step 4 — Start the stack

```bash
docker compose up --build
```

This starts:
- `app` — CartIQ Spring Boot server on `http://localhost:8080`
- `db` — PostgreSQL 16 on port `5432`
- `redis` — Redis 7 on port `6379`

Verify it's healthy:
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

### Step 5 — Expose localhost to the internet (ngrok)

Meta needs a public HTTPS URL to deliver webhook events:

```bash
ngrok http 8080
```

Copy the `https://xxxx.ngrok-free.app` URL.

Back in the Meta Developer Console → **WhatsApp → Configuration**:
- **Callback URL**: `https://xxxx.ngrok-free.app/webhook`
- **Verify token**: `cartiq-verify-token` (matches your `.env`)
- Click **Verify and Save**
- Subscribe to the **messages** webhook field

---

### Step 6 — Add your phone as a test recipient

In **WhatsApp → API Setup**, add your personal WhatsApp number as a recipient (Meta requires this for free-tier testing).

---

### Step 7 — Send your first message

Open WhatsApp and send a message to your registered test number:

| You send | CartIQ replies |
|----------|---------------|
| `hi` | Help menu |
| `location Delhi` | 📍 Location set |
| `milk, bread, eggs` | Full price comparison |
| `alert milk < 30` | ✅ Alert created |

---

## Bot Commands Reference

| Command | Example | What it does |
|---------|---------|-------------|
| Grocery list | `milk, bread, eggs` | Compare prices across all 3 platforms |
| Set location | `location Mumbai` | Personalise results for your city |
| Set alert | `alert milk < 60` | Notify when milk drops below ₹60 |
| Help | `hi` / `help` / `hello` | Show usage guide |

---

## Running Tests

```bash
./gradlew test
```

19 unit tests covering: DecisionEngine, NormalizationEngine, ResponseFormatter.

---

## Adding a New Platform

1. Create `client/MyPlatformClient.kt` implementing `PlatformClient`:
   ```kotlin
   @Component
   class MyPlatformClient : PlatformClient {
       override fun search(query: String, location: String): List<PriceResult> { ... }
   }
   ```
2. Add config keys to `application.yml` and `docker-compose.yml`
3. `PriceAggregator` auto-discovers all `PlatformClient` beans — no other changes needed ✅

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `WHATSAPP_PHONE_NUMBER_ID` | ✅ | — | Meta phone number ID |
| `WHATSAPP_ACCESS_TOKEN` | ✅ | — | Meta API access token |
| `WHATSAPP_VERIFY_TOKEN` | ✅ | `cartiq-verify-token` | Webhook verification token |
| `WHATSAPP_APP_SECRET` | ⚠️ Recommended | *(blank = skip validation)* | HMAC-SHA256 signature secret |
| `DATABASE_URL` | — | `jdbc:postgresql://localhost:5432/cartiq` | PostgreSQL URL |
| `DB_USER` | — | `qc` | DB username |
| `DB_PASSWORD` | — | `qcpass` | DB password |
| `REDIS_HOST` | — | `localhost` | Redis host |
| `REDIS_PORT` | — | `6379` | Redis port |

---

## Scaling Roadmap

| Phase | Users | Focus |
|-------|-------|-------|
| **MVP** | 0–100 | Mock platform adapters, single server |
| **V2** | ~1K | Real platform API / scraper clients |
| **V3** | ~10K | Horizontal scaling, connection pooling, rate limiting |
| **V4** | 100K+ | Event-driven (Kafka), ML-based item matching, multi-language |

---

## License

MIT
