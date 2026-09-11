# Grafana Cloud and Sentry for observability

- **Status:** Accepted
- **Relates to:** ADR-0012 (single-VPS compose deployment — the memory budget that rules
  out self-hosting), ADR-0015 (Kafka — the box's other recent memory-pressure addition,
  same constraint applies here)

The app is live at `pictogram.imshy.me` with no way to answer "is it up, what's slow, who
hit an error, how many people used it today" other than SSHing in and reading container
logs. Actuator exposes only `health` and `info`; Micrometer's HTTP/JVM meters exist but
nothing registers or exports them; the tracing bridge (`micrometer-tracing-bridge-brave` +
`spring-modulith-observability`) creates spans with no exporter, so they are dropped;
unhandled exceptions are logged once by `ApiExceptionHandler` and go nowhere else; the
frontend's `RouteError` boundary renders a message and reports it to nobody.

## Decision

Buy the infrastructure-observability pieces rather than build them. This is operational
tooling, not a domain concern — there is no modelling value in reimplementing an error
tracker or a metrics store, unlike `notifications` (ADR-0015), which is a real bounded
context because the data and its lifecycle are the app's own.

- **Grafana Cloud**, free tier — metrics and logs. A single **Grafana Alloy** container on
  the box scrapes Actuator's Prometheus endpoint and tails the app/chat/Caddy container
  logs, then ships both out. Storage and dashboards are hosted; the box only runs the
  shipping agent.
- **Sentry**, free tier — exception tracking, backend and frontend. Grouping, stack
  traces, and frontend source maps are exactly the parts not worth rebuilding.
- **Traces are not wired.** The dormant Brave bridge and `spring-modulith-observability`
  dependencies are left as-is; tracing earns its cost when there is a specific
  cross-module latency question to chase, not before.

## Why not self-host

ADR-0012's box is 4 GB and already runs two JVMs (`app`, `chat`), Postgres, MinIO, and a
heap-capped Kafka broker — ADR-0015 already calls this box "a known memory-pressure
point." A self-hosted Grafana + Prometheus + Loki + shipping-agent stack, even trimmed
(no Tempo, short retention), is roughly another 600 MB. Grafana Cloud's free tier avoids
that entirely: the box's only new resident process is Alloy (~150 MB), pushing data to
storage that isn't its problem. If the box is ever upgraded (#176), Alloy's remote-write
target can move to a self-hosted stack without touching application code — the choice
here is about where data lives, not how it's produced.

## Scope

- **HTTP and JVM metrics**: `micrometer-registry-prometheus` on `app` and `chat`,
  `/actuator/prometheus` exposed. No Caddy route is added for it — only
  `/actuator/health` is proxied externally (per the existing `Caddyfile.prod`), so the new
  endpoint is reachable only on the compose network, which is already this app's trust
  boundary for internal traffic. No separate auth layer.
- **Product metrics** (user/post/engagement counts, an activity gauge): a scheduled job in
  `app`, alongside the existing `OrphanMediaCollectionJob` pattern, publishes Micrometer
  gauges. It reads counts through a small public stats interface each owning module
  exposes (`identity`, `post`, `social`) rather than querying their schemas directly —
  cheap enough to keep honest with the `InternalSlicingTest` boundary guard, and it is the
  only new code this decision requires beyond wiring. "Active" is derived from
  `identity.refresh_token` issuance/rotation (a returning session), not from content
  creation — lurking still counts, posting is not required.
- **Logs**: Alloy tails the existing ECS-JSON console output of `app`, `chat`, and `caddy`
  and ships it to Loki.
- **Exceptions**: Sentry SDKs in both the backend (hooked into `ApiExceptionHandler`'s
  catch-all, since exceptions are caught there and never propagate to the container) and
  the frontend (hooked into `RouteError`).

## Privacy

Logs and exception events can carry emails, IPs, and user agents, and both vendors are
US-based third parties receiving that data for the first time since the privacy policy
(`frontend/public/privacy.html`) was published. PII is off by default rather than
disclosed and shipped wholesale:

- Sentry: `sendDefaultPii: false`; request bodies are not attached.
- Alloy: email and IP fields are stripped from log lines before they leave the box.
- Correlation uses the user UUID already present in tokens and domain data, not PII.
- The privacy policy gains a one-line subprocessors mention (Grafana Labs, Sentry).

## Consequences

- Two more free-tier SaaS accounts and their credentials to manage (`scripts/deploy/wizard.sh`
  gains prompts for them, alongside the existing DB/MinIO/signing-key secrets).
- Product-stat gauges are the one piece of this ADR that is real application code; a
  future consumer of those counts (a real in-app dashboard, should the app ever grow
  multiple operators) can read the same per-module stats interfaces instead of duplicating
  them.
- No trace data and no in-app admin view — both are within scope to revisit later without
  reopening this decision, since neither is precluded by it.
- If the free tiers are ever outgrown, this ADR's boundary is the shipping mechanism
  (Alloy → Grafana Cloud, SDK → Sentry), not the application code that produces the data,
  so switching backends is a config change.
