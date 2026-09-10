FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/resume-ats-bot-1.0.0.jar app.jar

EXPOSE 8080

# Secrets must be injected via platform environment variables (Railway/Fly.io/Koyeb).
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
