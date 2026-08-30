# syntax=docker/dockerfile:1
#
# The Pictogram deployable: one image serving the API and the built SPA at one port.
# Build context is the repo root (see compose.yaml) so both builds are in scope.

FROM node:24-alpine AS frontend
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
RUN corepack enable
WORKDIR /frontend
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile
COPY frontend/ ./
RUN pnpm build

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
