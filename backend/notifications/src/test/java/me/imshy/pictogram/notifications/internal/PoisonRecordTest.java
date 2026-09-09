package me.imshy.pictogram.notifications.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * A record the handler cannot process (here: an event type that is not on the
 * agreed contract) is retried three times, then published to
 * {@code pictogram.social.DLT} with its offset committed — the next record on
 * the same partition still flows — and the DLT listener logs it at ERROR
 * (ADR-0015).
 *
 * <p>
 * The property override reproduces production:
 * {@code spring-modulith-events-kafka} sets the app-wide producer
 * value-serializer to {@code ByteArraySerializer} for the relay, so the
 * dead-letter path must carry its own String-serialising producer.
 */
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(
    properties = "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.ByteArraySerializer")
class PoisonRecordTest extends NotificationsModuleIntegrationTest {

    private static final Instant WHEN = Instant.parse("2026-09-09T12:00:00Z");

    @MockitoSpyBean
    Notifications notifications;

    @Test
    void aPoisonRecordIsRetriedThenDeadLetteredWithoutBlockingThePartition(CapturedOutput output) {
        UUID recipient = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID post = UUID.randomUUID();

        // Same key → same partition. "post-unliked" is off the contract, so the handler
        // throws on it; the follow queued behind it must still be delivered once the
        // poison record is dead-lettered and its offset committed.
        publish(recipient.toString(), wire("post-unliked", recipient, actor, post));
        publish(recipient.toString(), wire("user-followed", recipient, actor, null));

        await().atMost(Duration.ofSeconds(40)).untilAsserted(() -> assertThat(notificationsFor(recipient))
            .singleElement().satisfies(n -> assertThat(n.type()).isEqualTo(NotificationType.USER_FOLLOWED)));

        // First delivery plus at least the three configured retries, all on the poison
        // record (a consumer-group rebalance mid-retry can reset the count and add
        // more).
        verify(notifications, atLeast(4)).record(argThat(event -> "post-unliked".equals(event.type())));
        assertThat(deadLetterValues()).anySatisfy(value -> assertThat(value).contains("post-unliked"));
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(output.getOut()).contains("ERROR").contains("dead-lettered"));
    }

    private static String wire(String type, UUID recipient, UUID actor, UUID subject) {
        String subjectField = subject == null ? "" : "\"subjectId\":\"" + subject + "\",";
        return "{\"type\":\"" + type + "\",\"recipientId\":\"" + recipient + "\",\"actorId\":\"" + actor + "\","
            + subjectField + "\"occurredAt\":\"" + WHEN + "\"}";
    }
}
