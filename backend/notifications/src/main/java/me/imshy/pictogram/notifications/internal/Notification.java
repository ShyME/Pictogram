package me.imshy.pictogram.notifications.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import org.springframework.data.domain.Persistable;

/**
 * One delivered notification: an {@code actor} did something of a {@code type}
 * to a {@code recipient}, optionally about a {@code subject} post, at
 * {@code occurredAt}. Written by the {@code pictogram.social} consumer; the
 * read side (#198) is the only thing that reads it back.
 *
 * <p>
 * Rows are created with a native insert-or-ignore
 * ({@link NotificationStore#insertIfNew}) rather than {@code save()}, so a
 * redelivered Kafka record cannot double-insert and cannot doom the consumer's
 * transaction with a constraint violation.
 */
@Entity
@Table(schema = "notification", name = "notification")
class Notification implements Persistable<UUID> {

    @Id
    private UUID id;

    @Convert(converter = NotificationTypeConverter.class)
    private NotificationType type;

    @Column(name = "recipient_id")
    private UUID recipientId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    private boolean read;

    @Column(name = "created_at")
    private Instant createdAt;

    @Transient
    private boolean persisted;

    protected Notification() {
    }

    NotificationType type() {
        return type;
    }

    UserId recipientId() {
        return new UserId(recipientId);
    }

    UserId actorId() {
        return new UserId(actorId);
    }

    PostId subjectId() {
        return subjectId == null ? null : new PostId(subjectId);
    }

    Instant occurredAt() {
        return occurredAt;
    }

    boolean read() {
        return read;
    }

    Instant createdAt() {
        return createdAt;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return !persisted;
    }

    @PostPersist
    @PostLoad
    void markPersisted() {
        this.persisted = true;
    }
}
