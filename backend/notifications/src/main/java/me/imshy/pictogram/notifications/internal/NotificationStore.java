package me.imshy.pictogram.notifications.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface NotificationStore extends ListCrudRepository<Notification, UUID> {

    /**
     * Insert one notification unless its natural key
     * {@code (type, recipient_id, actor_id, occurred_at)} is already present —
     * Kafka delivers at-least-once, so a redelivered record must be a no-op. Done
     * as a single {@code on conflict do nothing} statement rather than
     * check-then-{@code save()} so a genuine race never raises a
     * {@code DataIntegrityViolationException} into the consumer's transaction.
     *
     * @return 1 if a row was inserted, 0 if the key already existed
     */
    @Modifying
    @Query(nativeQuery = true, value = """
        insert into notification.notification
            (id, type, recipient_id, actor_id, subject_id, occurred_at, read, created_at)
        values
            (:id, :type, :recipientId, :actorId, cast(:subjectId as uuid), :occurredAt, false, :createdAt)
        on conflict on constraint notification_natural_key do nothing
        """)
    int insertIfNew(@Param("id") UUID id, @Param("type") String type, @Param("recipientId") UUID recipientId,
        @Param("actorId") UUID actorId, @Param("subjectId") UUID subjectId, @Param("occurredAt") Instant occurredAt,
        @Param("createdAt") Instant createdAt);

    /**
     * Purge every notification about a post — the #138-style {@code PostDeleted}
     * reaction.
     */
    @Transactional
    int deleteBySubjectId(UUID subjectId);
}
