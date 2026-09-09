package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import me.imshy.pictogram.post.PublishedPosts;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.social.PostLiked;
import me.imshy.pictogram.testsupport.DatabaseTruncationExtension;
import me.imshy.pictogram.testsupport.SharedKafka;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.messaging.Message;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Kafka outbox for {@code social}'s engagement events (ADR-0015), end to
 * end: a {@code PostLiked} published in a doomed transaction leaves no
 * publication row, and one whose relay dies before it forwards leaves an
 * incomplete row that
 * {@link IncompleteEventPublications#resubmitIncompletePublications} — the call
 * the {@code republish-outstanding-events-on-restart} hook makes on startup —
 * pushes through to {@code pictogram.social}. The only {@code app} test that
 * runs the real relay against the shared broker; every other slice starts with
 * no broker.
 */
@SpringBootTest(classes = PictogramApplication.class, properties = "spring.autoconfigure.exclude=")
@ActiveProfiles("test")
@Import(SharedWebTestConfig.class)
@ExtendWith(DatabaseTruncationExtension.class)
class SocialEventKafkaRelayTest {

    private static final PostId POST = PostId.random();
    private static final UserId AUTHOR = UserId.random();
    private static final ViewerId LIKER = ViewerId.random();
    private static final Instant WHEN = Instant.parse("2026-09-09T12:00:00Z");

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        SharedKafka.registerTo(registry);
    }

    @MockitoBean
    PublishedPosts publishedPosts;

    @MockitoSpyBean
    KafkaOperations<Object, Object> kafka;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    IncompleteEventPublications incompletePublications;

    @Autowired
    JdbcClient db;

    private final AtomicBoolean relayIsDown = new AtomicBoolean(true);

    @BeforeEach
    void postHasAKnownAuthor() {
        given(publishedPosts.authorOf(POST)).willReturn(Optional.of(AUTHOR));
    }

    @Test
    void aLikeWhoseRelayDiesBeforeForwardingIsResubmittedOnRestart() {
        // The relay's first send fails, as if the process died before the broker ack.
        doAnswer(invocation -> {
            if (relayIsDown.getAndSet(false)) {
                return CompletableFuture.failedFuture(new IllegalStateException("relay down"));
            }
            return invocation.callRealMethod();
        }).when(kafka).send(any(Message.class));

        new TransactionTemplate(transactionManager)
            .executeWithoutResult(status -> eventPublisher.publishEvent(new PostLiked(POST, LIKER, WHEN)));

        await().atMost(Duration.ofSeconds(20))
            .untilAsserted(() -> assertThat(incompletePublicationCount()).isEqualTo(1));

        try (var consumer = consumerAt("pictogram.social")) {
            incompletePublications.resubmitIncompletePublications(__ -> true);

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                ConsumerRecord<String, String> record = poll(consumer);
                assertThat(record).isNotNull();
                assertThat(record.key()).isEqualTo(AUTHOR.toString());
                assertThat(JsonMapper.builder().build().readValue(record.value(), Map.class))
                    .containsExactlyInAnyOrderEntriesOf(
                        Map.of("type", "post-liked", "recipientId", AUTHOR.toString(), "actorId", LIKER.toString(),
                            "subjectId", POST.toString(), "occurredAt", "2026-09-09T12:00:00Z"));
            });
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(incompletePublicationCount()).isZero());
    }

    @Test
    void aPostLikedPublishedInADoomedTransactionLeavesNoOutboxRow() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(new PostLiked(POST, LIKER, WHEN));
            status.setRollbackOnly();
        });

        assertThat(incompletePublicationCount()).isZero();
        assertThat(totalPublicationCount()).isZero();
    }

    private long incompletePublicationCount() {
        return db.sql("select count(*) from event_publication where completion_date is null").query(Long.class)
            .single();
    }

    private long totalPublicationCount() {
        return db.sql("select count(*) from event_publication").query(Long.class).single();
    }

    private static ConsumerRecord<String, String> poll(KafkaConsumer<String, String> consumer) {
        var records = consumer.poll(Duration.ofMillis(500));
        return records.isEmpty() ? null : records.iterator().next();
    }

    private static KafkaConsumer<String, String> consumerAt(String topic) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
            Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, SharedKafka.INSTANCE.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "relay-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest", ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
        consumer.subscribe(List.of(topic));
        return consumer;
    }
}
