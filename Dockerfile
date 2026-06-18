# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and dependency manifests first (layer-cache friendly)
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

# Download dependencies only (cached unless build files change)
RUN ./gradlew dependencies --no-daemon -q || true

# Copy source and build
COPY src ./src
RUN ./gradlew bootJar --no-daemon -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Non-root user for security
RUN addgroup -S cartiq && adduser -S cartiq -G cartiq
USER cartiq

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
