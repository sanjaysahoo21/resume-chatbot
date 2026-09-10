# ==========================================
# Stage 1: Build the application with Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copy only the pom.xml first to cache dependencies (speeds up future builds)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the actual source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Create the final lean image
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /build/target/resume-ats-bot-1.0.0.jar app.jar

EXPOSE 8080

# Secrets must be injected via platform environment variables (Render/Railway/Fly.io).
# Do NOT set real values here — this file is committed to git.
# Required env vars:
#   TELEGRAM_BOT_TOKEN
#   TELEGRAM_BOT_USERNAME
#   LLM_API_KEY
#   LLM_BASE_URL  (default: https://api.groq.com/openai/v1)
#   LLM_MODEL     (default: llama-3.3-70b-versatile)

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
