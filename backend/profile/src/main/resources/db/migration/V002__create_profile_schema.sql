-- profile owns the `profile` schema (ADR-0009). A profile is keyed by
-- the user's UserId value; there is no foreign key to identity.app_user — contexts
-- reference each other by ID value only (ADR-0002).
create schema if not exists profile;

-- One row per onboarded user. `username` matches ^[a-z0-9_]{3,20}$ (enforced in the domain)
-- and is unique — a lowercase-only handle, so a plain unique constraint is case-safe.
-- `display_name` and `bio` are optional and null when unset.
create table profile.profile
(
    user_id      uuid        primary key,
    username     text        not null unique,
    display_name text,
    bio          text,
    created_at   timestamptz not null
);
