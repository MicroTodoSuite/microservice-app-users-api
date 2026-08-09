FROM maven:3.9.9-eclipse-temurin-8 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

FROM eclipse-temurin:8-jre AS runtime

RUN groupadd --gid 10001 users-api \
    && useradd --uid 10001 --gid users-api --no-create-home --shell /usr/sbin/nologin users-api

WORKDIR /app
COPY --from=build --chown=users-api:users-api /app/target/*.jar app.jar

USER 10001:10001

EXPOSE 8083

# Shell form is required here for ${SERVER_PORT} expansion and the `|| exit 1` fallback.
# hadolint ignore=DL3025
HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=3 \
    CMD wget -qO- "http://localhost:${SERVER_PORT:-8083}/prometheus" || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
