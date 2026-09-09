# Notifications

The in-app notification list: a record, per recipient, that someone liked or commented on
one of their posts, or started following them. This context only **writes** that list —
one Kafka flow in, one in-process event reaction, rows in a table. The read side (the
`GET /api/notifications` API and the SPA bell) lands in #198–#199.

It is a Spring Modulith module inside the monolith even though its one producer (`social`)
runs in the same JVM. That is deliberate (ADR-0001 / ADR-0015): every context here is built
to be extractable, and this is the one whose extraction seam — a durable topic — already
exists. Extracting it later is a deployment change, not a rewrite.

## Glossary

**Notification**: one row saying an _actor_ did something of a _type_ to a _recipient_,
optionally about a _subject_, at a point in time. Created once and never edited; `read` is
the only mutable field (flipped in bulk by #198). Removed only when its subject post is
deleted.
_Avoid_: Alert, message, event, feed item

**Recipient**: the user the notification is _for_ — the post's author for a like or a
comment, the followed user for a follow. Carried on the `pictogram.social` payload
(resolved by `social` at publish time) and used as the Kafka partition key, so all of one
person's notifications are ordered on one partition.
_Avoid_: Target, owner, user, subscriber

**Actor**: the user who _caused_ the notification — the liker, the commenter, the follower.
Stored by id only; the SPA resolves the display name via `GET /api/profiles?ids=` (#199).
_Avoid_: Sender, author, source

**Subject**: the post a like or comment is about. A follow has no subject (`subject_id` is
null). Referenced by id only — no foreign key to `post` (ADR-0002).
_Avoid_: Object, target, resource

**Type**: `post-liked`, `post-commented` or `user-followed` — exactly the `type`
discriminators on the `pictogram.social` wire. A fourth value on the topic is a contract
break and is treated as a poison record.
_Avoid_: Kind, category, event type

**Self-action**: a like or comment by a user on their own post (actor == recipient).
Produces no notification.
_Avoid_: Self-notification, echo

## The consume-only integration

`social` externalises `PostLiked` / `PostCommented` / `UserFollowed` to the single
`pictogram.social` Kafka topic as a `SocialEvent` JSON payload keyed by recipient id (#196,
ADR-0015). This module consumes it with a **hand-written `spring-kafka` `@KafkaListener`**
(`SocialEventConsumer`) — hand-written, not Modulith's incoming side, so the consumer group
(`notifications`), the `concurrency = 3`, the manual ack and the error handling are all
visible in one class (and its `NotificationsKafkaConsumerAutoConfiguration`). Both
`pictogram.social` and `pictogram.social.DLT` are declared with 3 partitions so the three
consumer threads each own one and a recipient's notifications stay ordered.

- The listener reads each record as raw JSON and binds it to **its own**
  `SocialEventMessage` — the topic is the contract, not a shared class, exactly as a real
  service boundary would have it. `SocialEventMessageTest` pins the JSON it must accept
  (the mirror of `social`'s `SocialEventWireContractTest`).
- Per record: a self-action is skipped; otherwise a row is inserted with
  `NotificationStore.insertIfNew` — a single `insert … on conflict do nothing` on the
  natural key `(type, recipient_id, actor_id, occurred_at)`. Kafka delivers at-least-once,
  so a redelivered record must be a no-op, and `on conflict do nothing` (rather than
  check-then-`save()`) means a genuine race never raises a
  `DataIntegrityViolationException` into the consumer's transaction.
- The record is acked **only after** that write commits (the consumer owns the transaction
  via a `TransactionTemplate`, so nothing in `Notifications` is `@Transactional`). A crash
  before the commit redelivers the record; the insert-or-ignore absorbs it.

### The dead-letter topic

A handler that throws is retried by `DefaultErrorHandler` — 3 retry attempts after the
first delivery, backing off 1s / 2s / 4s — then the record is published to
**`pictogram.social.DLT`** and its offset is committed, so a poison record never blocks its
partition for longer than the backoff. The dead-letter producer is a **dedicated
`KafkaTemplate`**: the app's shared one is set to `ByteArraySerializer` by
`spring-modulith-events-kafka` for the relay, and these records carry `String` values.
`SocialEventConsumer.onDeadLetter` is the DLT's only reader — it logs each dead-lettered
record at ERROR and nothing else, on its own container factory so a failure there can never
be re-published onto the DLT it came from. Draining the DLT is a runbook action; there
is no reprocessing here.

## The PostDeleted reaction

`Notifications.onPostDeleted` consumes `post`'s `PostDeleted` in-process — a synchronous
`@EventListener` in the deleting transaction, the same pattern `social`'s comment thread
uses (#138) — and deletes that post's notifications, so the common case (a post deleted
after its likes and comments have settled) leaves no notification pointing at a 404. It is
best-effort, not a guarantee: a `PostLiked` / `PostCommented` still in flight on Kafka when
the post is deleted is consumed afterwards and re-creates one orphan — the same class of
cosmetic staleness ADR-0015 already accepts for undo events. This is the only in-process
integration; everything else about this module comes over Kafka.

## What is deliberately not here

- **Undo events.** `PostUnliked`, `CommentDeleted` and `UserUnfollowed` are neither
  externalised by `social` (#196) nor consumed here. A stale "X liked your post" after an
  unlike is a cosmetic inconsistency v1 accepts (ADR-0015).
- **A published interface.** Nothing outside `notifications` queries it. `Notifications`,
  the store and the consumer are all package-private in `internal`.
- **Per-item read state, and any HTTP surface.** Both are #198.

## Schema

The `notification` schema (ADR-0009), one table `notification.notification`. The natural
key is a unique constraint on `(type, recipient_id, actor_id, occurred_at)` — `recipient_id`
rather than the `subject_id` the ADR first sketched, because a follow has no subject and an
all-non-null tuple lets Postgres treat a redelivery as a real duplicate without
`NULLS NOT DISTINCT`. Indexed on `subject_id` (the `PostDeleted` purge) and on
`(recipient_id, created_at desc, id desc)` (the #198 keyset read).

## Testing

This is the **only** module whose `./gradlew :notifications:test` starts a container — the
shared Kafka broker (`SharedKafka`, ADR-0015). The shared test profile turns the Kafka
client autoconfig off for every other slice; `NotificationsModuleIntegrationTest`
re-includes it. The unit tests (`SocialEventMessageTest`, `NotificationTypeTest`) need no
broker.
