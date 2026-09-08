# Kafka for notification delivery

- **Status:** Accepted
- **Relates to:** ADR-0002 (intra-monolith integration — this ADR *narrows* it, does not
  supersede it), ADR-0001 (every context is a Modulith module drawn for later
  extraction), ADR-0003 (fan-out-on-write, the other event-choreography evolution — not
  this one), ADR-0009 (schema per module), ADR-0012 (single-VPS compose deployment)

ADR-0002 deferred an external message broker until "there is an async consumer whose loss
actually matters." In-app **notifications** are that consumer: a like, comment, or follow
should reach the recipient even if the delivering process restarts mid-handler, the
producing transaction must not wait on it, and the read side is a different module's
concern with a different write rate. We add **Apache Kafka** for this one externalised
flow and introduce a **`notifications`** bounded context that consumes it.

This does **not** replace Spring Modulith application events. Intra-monolith integration
is unchanged — synchronous published-interface queries and in-process events, exactly as
ADR-0002 describes. Kafka carries precisely three event types, out of `social`, into
`notifications`, and nothing else.

## What is externalised, and how

- `social` keeps publishing `PostLiked`, `PostCommented` and `UserFollowed` as ordinary
  application events. Each is additionally annotated `@Externalized("pictogram.social")`.
- **Spring Modulith event externalisation** (`spring-modulith-events-kafka`) publishes the
  annotated events to Kafka. The **JPA event-publication registry**
  (`spring-modulith-events-jpa`) is the outbox: a publication row is written in the *same
  transaction* as the like/comment/follow, a relay forwards it to Kafka and marks it
  complete, and incomplete publications are resubmitted on startup. No hand-rolled outbox
  table — the registry already is one.
- The **consumer is hand-written `spring-kafka`** (`@KafkaListener` in `notifications`),
  not Modulith's incoming side, so consumer-group, offset and error-handler behaviour is
  explicit and visible.

Considered and rejected: a hand-rolled `outbox` table with a `@Scheduled` poller
(reimplements the registry against a project that already depends on Modulith); a plain
`@TransactionalEventListener(AFTER_COMMIT)` calling `KafkaTemplate` (no outbox — a crash
between commit and send silently drops the notification).

## Topic and delivery

- **One topic**, `pictogram.social`, JSON payload with a `type` discriminator field. One
  topic keeps the wire contract in one place; the three event shapes are distinguished by
  the field, not by topic.
- **Partition key is the target user id** — the *recipient* (post author, or the followed
  user), not the actor. All of one recipient's notifications land on one partition, so
  they are ordered and handled by a single consumer; a heavily-liked account cannot
  reorder or starve another's. This is the non-obvious choice a reviewer would question.
  Partitioning by recipient means the **producer** must know the recipient at publish
  time: `social` resolves a post's author via `PublishedPosts.authorOf` when it
  externalises a `PostLiked` / `PostCommented`, and puts the recipient id on the payload.
  The event is then carried whole — the consumer does not call back into `post`.
- **3 partitions**, one consumer group `notifications`, `auto-offset-reset=earliest`,
  container `concurrency=3`, manual ack after the handler transaction commits.
- **At-least-once delivery.** The consumer dedups on a unique constraint over the natural
  composite `(type, subject_id, actor_id, occurred_at)` — insert-or-ignore. The producer
  events gain **no `eventId` field**: dedup is the consumer's concern and belongs in the
  consumer's schema (a producer should not grow a field for one consumer). If a second
  externalised consumer ever appears, revisit and add an explicit event id then.
- **Retry / dead-letter:** `DefaultErrorHandler` with exponential backoff (1s, 2s, 4s),
  3 attempts, then the record is published to `pictogram.social.DLT` and the offset is
  committed — a poison message never blocks its partition. A logging-only listener on the
  DLT surfaces it at ERROR. Draining the DLT is a runbook action; there is no
  reprocessing UI.

## The `notifications` context

- New `backend/notifications/` Gradle subproject: one Spring Modulith module, its own
  Flyway `notification` schema (ADR-0009), an `internal/` slice under the
  `InternalSlicingTest` guard. No published interface — nothing queries it.
- It runs **inside the monolith** and consumes from Kafka even though the producer is in
  the same JVM. That is deliberate and consistent with ADR-0001: every context here is
  built to be extractable, and `notifications` is the one whose extraction seam
  (a durable topic) already exists. Extracting it later is a deployment change, not a
  rewrite.
- The consumer reads the recipient straight off the payload (resolved by `social` at
  publish time, above) — no synchronous call back into `post`.
- **Self-actions are suppressed** (liking or commenting on your own post produces no
  notification).
- **Undo events are ignored.** `PostUnliked`, `CommentDeleted` and `UserUnfollowed` are
  not externalised and not consumed. A stale "X liked your post" after an unlike is a
  cosmetic inconsistency every real app carries; consuming undos would double the consumer
  logic for little signal. Deliberate v1 cut.
- **`PostDeleted` is consumed** to purge that post's notifications, reusing the existing
  cross-context `PostDeleted` listener pattern (#138). This one keeps the data honest
  because an orphaned notification links to a 404.

## Read side

- `GET /api/notifications` — keyset-paged, newest first, `shared.http.Cursor` and the
  ADR-0008 envelope, same as the feed and profile grid.
- `GET /api/notifications/unread-count` → `{ "count": n }`.
- `POST /api/notifications/mark-read` — marks all of the caller's notifications read.
  No per-item read state in v1 (deliberate cut).
- The list response carries `actorId`, not an enriched actor. The SPA batch-resolves it
  with `GET /api/profiles?ids=…` (ADR-0005), so `notifications` gains no dependency on
  `profile`.
- Frontend: a bell with an unread badge in `AppNav`, polling `unread-count` every 30s
  (no WebSocket — chat's socket infrastructure is a separate service per ADR-0014, and
  notifications do not need sub-second latency); a `/notifications` screen that lists them
  and calls `mark-read` on mount. Rows link to the post, or to the profile for a follow.

## Serialisation

JSON via Jackson. A `pictogram.social` wire-shape contract test pins the serialised form
of all three externalised events, extending the cross-module consumer-contract pattern
(#157) to the cross-broker contract. No Schema Registry in v1 — Avro plus a registry
container is real value but can be layered on in a later ADR without changing this one's
shape.

## Deployment

Kafka (`apache/kafka-native` — the GraalVM native image, KRaft, no ZooKeeper) is a
container in both `compose.yaml` and the production `compose.prod.yaml` overlay, with its
JVM/heap explicitly capped. ADR-0012's box is 4 GB and already accounts for two JVMs plus
Postgres and MinIO, so Kafka is a known memory-pressure point — called out here and tied
to the #176 migration off the temporary GCP host, mirroring how ADR-0012's own addendum
handles that box. If the box genuinely cannot hold it, the fallback is to run Kafka in
local and CI environments only (Testcontainers) and gate the `notifications` consumer
behind a profile; the feature is then laptop-and-CI only until the box grows. The
intent is to run it for real.

## Testing

- **Producer side (`social`):** Spring Modulith's `Scenario` / `PublishedEvents` support
  asserts that a like externalises a `PostLiked`, with **no broker** in the test.
- **Consumer side (`notifications`):** integration tests against a **singleton
  Testcontainers Kafka** (one container for the module, `@ServiceConnection`) — produce a
  record onto `pictogram.social`, assert the `notification` row, assert dedup on
  redelivery, assert DLT routing for a poison record. This is the **only** Gradle module
  in the build that starts a Kafka container.
- **Wire contract test** as above.
- **Blackbox (post-merge, ADR-0007):** the real compose path; the notification assertion
  uses Awaitility polling because the flow is now asynchronous.
- **Other module slices** (`profile`, `post`, …) exclude `KafkaAutoConfiguration` and the
  Modulith Kafka externalisation in their test base, so they start without a broker.

## Consequences

- One more container on the box, and one more piece of infrastructure in the runbook —
  the cost of a real broker, accepted for one flow with a stated reason rather than
  adopted wholesale.
- Notification delivery is now eventually consistent. A test that liked a post and
  immediately asserted a notification must now poll.
- ADR-0002 still governs all intra-monolith integration and is unchanged. The honest
  narrative: the broker was deferred until a use case justified it, then added
  deliberately and narrowly.
- `task seed` creates likes, follows and comments; with Kafka running during the seed
  these now produce real notifications for the demo personas — signing in as `alice`
  shows a populated bell. No change to the seed script.
- A second externalised consumer would force the `eventId` question (deferred above) and
  make the single-topic / single-schema choice worth revisiting.
