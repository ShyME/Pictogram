# syntax=docker/dockerfile:1
#
# Custom Caddy image for the public deployment (ADR-0012). Stock `caddy:2-alpine` has no
# rate-limit module, and the deployment enforces per-IP limits at the edge (ADR-0011).
# `xcaddy` rebuilds the binary with caddy-ratelimit compiled in; the config path, data
# dir and entrypoint are all the upstream image's defaults.
FROM caddy:2-builder-alpine AS build
RUN xcaddy build --with github.com/mholt/caddy-ratelimit

FROM caddy:2-alpine
COPY --from=build /usr/bin/caddy /usr/bin/caddy
