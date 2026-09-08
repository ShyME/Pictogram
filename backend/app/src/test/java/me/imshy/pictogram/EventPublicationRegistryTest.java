package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.events.IncompleteEventPublications;

/**
 * The JPA event-publication registry is the outbox for the Kafka-externalised
 * flow (ADR-0015). It is JPA-mapped with {@code ddl-auto=none}, so the
 * {@code event_publication} table exists only because
 * {@code V011__create_event_publication_registry.sql} creates it — this test
 * fails if that migration is dropped or its shape drifts from what Modulith
 * maps.
 */
@AppIntegrationTest
class EventPublicationRegistryTest {

    @Autowired
    JdbcClient db;

    @Autowired
    IncompleteEventPublications incompletePublications;

    @Test
    void theRegistryTableIsCreatedByFlyway() {
        var columns = db
            .sql("select column_name from information_schema.columns where table_name = 'event_publication'")
            .query(String.class).list();

        assertThat(columns).contains("id", "listener_id", "event_type", "serialized_event", "publication_date",
            "completion_date", "status", "completion_attempts", "last_resubmission_date");
    }

    @Test
    void theRegistryIsWiredAndReadsCleanly() {
        assertThatCode(() -> incompletePublications.resubmitIncompletePublications(__ -> false))
            .doesNotThrowAnyException();
    }
}
