# syntax=docker/dockerfile:1
#
# The chat deployable: a standalone Spring WebFlux service (ADR-0014), built from its own
# Gradle build — no dependency on backend/. Build context is chat/ (see compose.yaml).

FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /src
COPY . ./
# No --mount=type=cache here (unlike backend/Dockerfile): ChatDockerImageTest builds this
# file through Testcontainers, whose classic builder can't run BuildKit-only instructions.
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S pictogram && adduser -S -D -H -G pictogram -u 1001 pictogram
COPY --from=build /src/build/libs/chat.jar ./app.jar
USER pictogram
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
