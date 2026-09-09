-- The `notifications` context owns the `notification` schema (ADR-0009, ADR-0015). A row
-- records that an actor did something to a recipient; the actor, recipient and subject are
-- referenced by their id value only — no foreign key across a schema boundary (ADR-0002).
create schema if not exists notification;

-- One row per delivered notification. `subject_id` is the liked / commented post and is
-- null for a follow. `read` is per-recipient, flipped in bulk by the read API (#198); v1
-- has no per-item read state.
create table notification.notification
(
    id           uuid        primary key,
    type         text        not null,
    recipient_id uuid        not null,
    actor_id     uuid        not null,
    subject_id   uuid,
    occurred_at  timestamptz not null,
    read         boolean     not null default false,
    created_at   timestamptz not null,

    -- Kafka delivers at-least-once; the consumer inserts-or-ignores on this key so a
    -- redelivered record is a no-op. `recipient_id` rather than `subject_id` (the key the
    -- ADR first sketched) keeps every column non-null — a follow has no subject — so
    -- Postgres treats the tuple as a real duplicate without NULLS NOT DISTINCT.
    constraint notification_natural_key unique (type, recipient_id, actor_id, occurred_at)
);

-- The #138-style PostDeleted purge is `delete ... where subject_id = ?`.
create index notification_by_subject_idx on notification.notification (subject_id);

-- The #198 read is `where recipient_id = ? order by created_at desc, id desc` (keyset).
create index notification_recipient_page_idx on notification.notification (recipient_id, created_at desc, id desc);
