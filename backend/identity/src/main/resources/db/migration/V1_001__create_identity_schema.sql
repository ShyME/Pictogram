-- identity owns the `identity` schema. Other contexts reference a user by UserId value
-- only, never by a cross-schema foreign key (ADR-0002), so nothing here points outward.
create schema if not exists identity;

-- What identity needs to authenticate a person and nothing more (see identity/CONTEXT.md).
-- `google_sub` is the stable Google subject; it is the natural key we look a returning
-- person up by. `provider` is here from day one as the IdentityProvider seam (ADR-0004):
-- a second provider (email/password) adds rows with a different value, no schema change.
create table identity.app_user
(
    id            uuid        primary key,
    provider      text        not null default 'google',
    subject       text        not null,
    email         text        not null,
    registered_at timestamptz not null,
    unique (provider, subject)
);

-- One row per issued refresh token. Tokens are stored only as a SHA-256 hash. A `family`
-- is the rotation chain minted at sign-in: each refresh consumes its row and issues the
-- next in the same family. Presenting a token whose row is already consumed or revoked is
-- treated as theft and revokes the whole family (ADR-0004 reuse detection).
create table identity.refresh_token
(
    id          uuid        primary key,
    family_id   uuid        not null,
    user_id     uuid        not null,
    token_hash  text        not null unique,
    issued_at   timestamptz not null,
    expires_at  timestamptz not null,
    consumed_at timestamptz,
    revoked_at  timestamptz
);

create index refresh_token_family_idx on identity.refresh_token (family_id);
