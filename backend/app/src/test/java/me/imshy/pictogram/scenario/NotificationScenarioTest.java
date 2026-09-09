package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import me.imshy.pictogram.scenario.PictogramApp.Notification;
import me.imshy.pictogram.testsupport.SharedKafka;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * The notification read side (#198), end to end over Kafka: Bob likes Alice's
 * post, Alice sees one unread notification carrying Bob's id, and mark-read
 * takes her count back to zero.
 *
 * <p>
 * The one fast scenario aggregator booted with the broker wired
 * ({@link SharedKafka}, ADR-0015) — the {@code social -> notifications} hop
 * runs over Kafka. Every other in-process scenario starts broker-free
 * ({@link SpringBootAppTests}); the empty {@code spring.autoconfigure.exclude}
 * re-includes the relay and the consumer that {@code application-test.yml}
 * switches off by name.
 */
@TestPropertySource(properties = "spring.autoconfigure.exclude=")
class NotificationScenarioTest extends AppIntegrationTest {

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        SharedKafka.registerTo(registry);
    }

    @Test
    void aLikeArrivesAsAnUnreadNotificationThatMarkReadClears() {
        var alice = pictogram().registerViaGoogle("alice@example.com");
        alice.completeOnboarding("alice_notif", "Alice", null);
        var bob = pictogram().registerViaGoogle("bob@example.com");
        String bobId = bob.completeOnboarding("bob_notif", "Bob", null).userId();
        String post = alice.publishPost(alice.uploadPhoto(JpegPhoto.some()), "a photo").postId();

        bob.like(post);

        await().atMost(Duration.ofSeconds(20))
            .untilAsserted(() -> assertThat(alice.unreadNotificationCount()).isEqualTo(1));

        assertThat(alice.openNotifications().notifications()).singleElement().satisfies(notification -> {
            assertThat(notification.type()).isEqualTo("post-liked");
            assertThat(notification.actorId()).isEqualTo(bobId);
            assertThat(notification.subjectPostId()).isEqualTo(post);
            assertThat(notification.read()).isFalse();
        });

        alice.markNotificationsRead();

        assertThat(alice.unreadNotificationCount()).isZero();
        assertThat(alice.openNotifications().notifications()).singleElement().extracting(Notification::read)
            .isEqualTo(true);
    }
}
