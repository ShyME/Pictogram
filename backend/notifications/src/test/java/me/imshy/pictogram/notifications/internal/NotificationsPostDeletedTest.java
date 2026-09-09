package me.imshy.pictogram.notifications.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.post.PostDeleted;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * {@code notifications} reacts to {@code post}'s {@code PostDeleted} the same
 * way {@code social}'s comment thread does (#138): synchronously, in the
 * deleting transaction, purging exactly that post's notifications (ADR-0015).
 */
class NotificationsPostDeletedTest extends NotificationsModuleIntegrationTest {

    @Autowired
    ApplicationEventPublisher events;

    @Test
    void deletingAPostPurgesOnlyThatPostsNotifications() {
        UUID recipient = UUID.randomUUID();
        UUID deletedPost = UUID.randomUUID();
        UUID otherPost = UUID.randomUUID();
        givenNotification(NotificationType.POST_LIKED, recipient, UUID.randomUUID(), deletedPost);
        givenNotification(NotificationType.POST_COMMENTED, recipient, UUID.randomUUID(), otherPost);

        events.publishEvent(new PostDeleted(new PostId(deletedPost), UserId.random(), MediaId.random(), Instant.now()));

        assertThat(notificationsFor(recipient)).singleElement()
            .satisfies(n -> assertThat(n.subjectId().value()).isEqualTo(otherPost));
    }
}
