-- Enforces a per-user storage cap (pictogram.media.quota.max-bytes-per-user): size_bytes is
-- the combined byte length of the original + thumbnail renditions stored for this media,
-- summed per owner at upload time. Existing rows backfill to 0 — there is no real byte count
-- to recover for them, and this is a dev/early-stage app with no real uploads yet to undercount.
alter table media.media
    add column size_bytes bigint not null default 0;

alter table media.media
    alter column size_bytes drop default;
