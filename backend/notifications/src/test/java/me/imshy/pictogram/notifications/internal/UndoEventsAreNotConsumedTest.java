package me.imshy.pictogram.notifications.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.PostUnliked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Undo events are deliberately not handled — {@code social} does not
 * externalise {@code PostUnliked} / {@code CommentDeleted} /
 * {@code UserUnfollowed} (#196), and {@code notifications} has no listener for
 * them either. A stale "X liked your post" after an unlike is a cosmetic
 * inconsistency v1 accepts (ADR-0015).
 */
class UndoEventsAreNotConsumedTest extends NotificationsModuleIntegrationTest {

    @Autowired
    ApplicationEventPublisher events;

    @Test
    void anInProcessUndoEventLeavesEveryNotificationInPlace() {
        UUID recipient = UUID.randomUUID();
        UUID post = UUID.randomUUID();
        givenNotification(NotificationType.POST_LIKED, recipient, UUID.randomUUID(), post);

        events.publishEvent(new PostUnliked(new PostId(post), ViewerId.random(), Instant.now()));

        assertThat(notificationsFor(recipient)).hasSize(1);
    }
}
