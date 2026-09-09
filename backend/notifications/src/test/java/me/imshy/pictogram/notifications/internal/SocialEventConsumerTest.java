package me.imshy.pictogram.notifications.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

/**
 * The {@code pictogram.social} consumer, end to end against the broker: each of
 * the three event types becomes a notification keyed to the recipient carried
 * on the payload, a self-action becomes nothing, and a redelivered record is a
 * no-op (ADR-0015).
 */
class SocialEventConsumerTest extends NotificationsModuleIntegrationTest {

    private static final Instant WHEN = Instant.parse("2026-09-09T12:00:00Z");

    @Autowired
    private KafkaListenerEndpointRegistry listeners;

    @Test
    void theConsumerRunsAtConcurrencyThree() {
        var container = (ConcurrentMessageListenerContainer<?, ?>) listeners
            .getListenerContainer(SocialEventConsumer.LISTENER_ID);

        assertThat(container.getConcurrency()).isEqualTo(3);
    }

    @Test
    void aLikeBecomesANotificationForThePostAuthor() {
        UUID author = UUID.randomUUID();
        UUID liker = UUID.randomUUID();
        UUID post = UUID.randomUUID();

        publish(author.toString(), wire("post-liked", author, liker, post, WHEN));

        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> assertThat(notificationsFor(author)).singleElement().satisfies(n -> {
                assertThat(n.type()).isEqualTo(NotificationType.POST_LIKED);
                assertThat(n.actorId().value()).isEqualTo(liker);
                assertThat(n.subjectId().value()).isEqualTo(post);
                assertThat(n.occurredAt()).isEqualTo(WHEN);
                assertThat(n.read()).isFalse();
            }));
    }

    @Test
    void aCommentBecomesANotificationForThePostAuthor() {
        UUID author = UUID.randomUUID();
        UUID commenter = UUID.randomUUID();
        UUID post = UUID.randomUUID();

        publish(author.toString(), wire("post-commented", author, commenter, post, WHEN));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(notificationsFor(author)).singleElement()
            .satisfies(n -> assertThat(n.type()).isEqualTo(NotificationType.POST_COMMENTED)));
    }

    @Test
    void aFollowBecomesANotificationWithNoSubject() {
        UUID followed = UUID.randomUUID();
        UUID follower = UUID.randomUUID();

        publish(followed.toString(), wire("user-followed", followed, follower, null, WHEN));

        await().atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> assertThat(notificationsFor(followed)).singleElement().satisfies(n -> {
                assertThat(n.type()).isEqualTo(NotificationType.USER_FOLLOWED);
                assertThat(n.subjectId()).isNull();
            }));
    }

    @Test
    void likingYourOwnPostProducesNoNotification() {
        UUID author = UUID.randomUUID();
        UUID post = UUID.randomUUID();

        publish(author.toString(), wire("post-liked", author, author, post, WHEN));
        // A follow-up event for the same recipient we can positively wait on, so the
        // self-action has definitely been consumed and skipped by the time we assert.
        publish(author.toString(), wire("user-followed", author, UUID.randomUUID(), null, WHEN));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(notificationsFor(author)).singleElement()
            .satisfies(n -> assertThat(n.type()).isEqualTo(NotificationType.USER_FOLLOWED)));
    }

    @Test
    void aRedeliveredRecordDoesNotDoubleInsert() {
        UUID author = UUID.randomUUID();
        UUID liker = UUID.randomUUID();
        UUID post = UUID.randomUUID();
        String record = wire("post-liked", author, liker, post, WHEN);

        publish(author.toString(), record);
        publish(author.toString(), record);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(notificationsFor(author)).hasSize(1));
        // Hold long enough that a second insert would have shown up.
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(4))
            .untilAsserted(() -> assertThat(notificationsFor(author)).hasSize(1));
    }

    private static String wire(String type, UUID recipient, UUID actor, UUID subject, Instant occurredAt) {
        String subjectField = subject == null ? "" : "\"subjectId\":\"" + subject + "\",";
        return "{\"type\":\"" + type + "\",\"recipientId\":\"" + recipient + "\",\"actorId\":\"" + actor + "\","
            + subjectField + "\"occurredAt\":\"" + occurredAt + "\"}";
    }
}
