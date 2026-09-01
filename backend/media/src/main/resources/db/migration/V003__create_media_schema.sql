-- media owns the `media` schema (ADR-0009). A media is keyed by its
-- MediaId value; there is no foreign key to identity.app_user — contexts reference each
-- other by ID value only (ADR-0002).
create schema if not exists media;

-- One row per uploaded image. The bytes live in object storage under keys derived from the
-- id by convention (media/CONTEXT.md); the content type and dimensions are the same
-- constants for every row (ADR-0006), so they are not stored. `owner_id` is the user who
-- uploaded it — the only thing the published interface exposes beyond existence.
create table media.media
(
    id         uuid        primary key,
    owner_id   uuid        not null,
    created_at timestamptz not null
);

create index media_owner_idx on media.media (owner_id);
