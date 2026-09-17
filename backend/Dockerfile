# syntax=docker/dockerfile:1
#
# The Pictogram deployable: one image serving the API and the built SPA at one port.
# Build context is the repo root (see compose.yaml) so both builds are in scope.

FROM node:24-alpine AS frontend
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
RUN corepack enable
WORKDIR /frontend
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile
COPY frontend/ ./
# ADR-0016: DSN is baked into the bundle at build time; org/project/token drive the Sentry
# Vite plugin's source-map upload, a no-op build when the token is absent (local/CI). The
# token is a secret — mounted, never an ARG, so it never lands in an image layer or its
# history.
ARG VITE_SENTRY_DSN
ARG SENTRY_ORG
ARG SENTRY_PROJECT
ENV VITE_SENTRY_DSN=${VITE_SENTRY_DSN} \
    SENTRY_ORG=${SENTRY_ORG} \
    SENTRY_PROJECT=${SENTRY_PROJECT}
RUN --mount=type=secret,id=sentry_auth_token \
    SENTRY_AUTH_TOKEN="$(cat /run/secrets/sentry_auth_token 2>/dev/null || true)" pnpm build

FROM eclipse-temurin:25-jdk-alpine AS backend
WORKDIR /src
COPY backend/ ./
# The SPA ships inside the jar: Spring serves it from classpath:/static/.
COPY --from=frontend /frontend/dist/ ./app/src/main/resources/static/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :app:bootJar

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S pictogram && adduser -S -D -H -G pictogram -u 1001 pictogram
COPY --from=backend /src/app/build/libs/pictogram.jar ./app.jar
USER pictogram
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
