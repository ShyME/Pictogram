# Chat: a separate service, coupled to the monolith only by JWT verification

- **Status:** Accepted
- **Relates to:** ADR-0001 (every other context is a Modulith module), ADR-0002 (no
  broker, Modulith events — scoped to intra-monolith integration), ADR-0004 (Pictogram
  JWTs, verifiable with the public key by an extracted service), ADR-0012 (single-VPS
  compose deployment)

Chat (1:1 direct messages, live-only, no history in v1) ships as a genuinely separate
deployable from day one — not an existing context later split out, but the first context
that never enters the Spring Modulith monolith at all. It is also a deliberate practice
target for reactive programming: the backend is Spring Boot **WebFlux**, not the Spring
MVC stack the rest of `:app` runs, and the frontend models its WebSocket as an RxJS
stream. Neither choice is required by chat's actual load (single-digit users, per
ADR-0012) — both are chosen to exercise the reactive style end to end.

Chat's only coupling to the monolith is verifying a Pictogram access token's signature
with the public key, exactly the extraction seam ADR-0004 was built for. It knows a
caller only as the `sub` claim (a `UserId`) — no database, no synchronous call to
`profile` or `social`, no Modulith event consumption. Display data (name, avatar) is
resolved client-side, by a frontend that already has it from wherever the chat was
opened, rather than chat calling `profile` to enrich messages server-side.

There is no message persistence in v1: a message to a recipient who isn't currently
connected is reported to the sender as undelivered, not queued or retried. To make that
signal — and an online/offline presence indicator — mean something more useful than "had
this exact window open," a client holds **one WebSocket connection per signed-in
session**, opened at the app shell (alongside `AppNav`, #135) rather than per
conversation; it fans out to every open tab. The access token is carried through the
WebSocket handshake via `Sec-WebSocket-Protocol`, not a query parameter, so it doesn't
land in Caddy's or a proxy's access logs.

Deployment does not change ADR-0012's shape: chat is another container in the same
`compose.yaml`, routed by Caddy on the same box — no new infrastructure, and no database
is provisioned for it in v1.

## Consequences

- Two Gradle builds, two CI jobs, two Docker images to maintain instead of one — the cost
  of practising a real extraction rather than deferring it like every other context here.
- Chat cannot react to monolith events (a `ProfileUpdated`, say) because it consumes
  none — acceptable because it needs nothing from the monolith beyond "is this token
  valid," but a real constraint if chat's scope grows to need more.
- Presence and delivery are both best-effort and hold no state across a reload: closing
  the tab drops the connection, and with it any in-flight undelivered-message signal.
- Message history, when it ships, means adding a datastore to chat from scratch — there
  is nothing to retrofit, which is the point of leaving it out now rather than
  provisioning an empty database speculatively.
