-- The Spring Modulith JPA event-publication registry — the outbox for events externalised to
-- Kafka (ADR-0015). A publication row is written in the same transaction as the domain change;
-- a relay forwards it to the broker and stamps completion_date; incomplete rows are resubmitted
-- on startup. Owned by the composition root, which is where externalisation is wired — no
-- domain module reads or writes it.
--
-- Shape is Modulith 2.1's v2 schema (spring-modulith-events-jdbc schemas/v2/schema-postgresql.sql),
-- reproduced here because this project runs Flyway with ddl-auto=none and does not use Modulith's
-- own schema initialisation. Keep it in sync when bumping the Modulith BOM.
create table event_publication
(
    id                     uuid                     not null primary key,
    listener_id            text                     not null,
    event_type             text                     not null,
    serialized_event       text                     not null,
    publication_date       timestamptz              not null,
    completion_date        timestamptz,
    status                 text,
    completion_attempts    int,
    last_resubmission_date timestamptz
);

create index event_publication_serialized_event_hash_idx on event_publication using hash (serialized_event);
create index event_publication_by_completion_date_idx on event_publication (completion_date);
