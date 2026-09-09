package me.imshy.pictogram.notifications.internal;

import java.time.Clock;
import java.util.UUID;
import me.imshy.pictogram.post.PostDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Turns each consumed {@code pictogram.social} event into a stored
 * notification, and purges a post's notifications when the post is deleted.
 *
 * <p>
 * No published interface — nothing outside {@code notifications} calls this.
 * The {@code pictogram.social} consumer owns the transaction boundary (it acks
 * the Kafka record only after the write commits), so nothing here is
 * {@code @Transactional}; {@link #record} runs inside the caller's transaction.
 */
@Service
class Notifications {

    private final NotificationStore store;
    private final Clock clock;

    Notifications(NotificationStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    void record(SocialEventMessage event) {
        if (event.isSelfAction()) {
            return;
        }
        NotificationType type = NotificationType.fromWire(event.type());
        UUID subjectId = event.subjectId() == null ? null : event.subjectId().value();
        store.insertIfNew(UUID.randomUUID(), type.wireName(), event.recipientId().value(), event.actorId().value(),
            subjectId, event.occurredAt(), clock.instant());
    }

    @EventListener
    void onPostDeleted(PostDeleted event) {
        store.deleteBySubjectId(event.postId().value());
    }
}
