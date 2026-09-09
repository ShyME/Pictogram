package me.imshy.pictogram.notifications.internal;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.imshy.pictogram.testsupport.ModuleIntegrationTest;
import me.imshy.pictogram.testsupport.SharedKafka;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Base for the {@code notifications} consumer suite: the real
 * {@code @KafkaListener} against the shared broker (the only module slice that
 * needs one — ADR-0015). The shared test profile excludes
 * {@code KafkaAutoConfiguration} and this module's consumer autoconfig for
 * every other slice; this base overrides the exclude list to re-include both,
 * keeping only the Modulith externaliser (the producer relay, irrelevant to a
 * consumer) off.
 */
@ApplicationModuleTest
@TestPropertySource(
    properties = "spring.autoconfigure.exclude=org.springframework.modulith.events.kafka.KafkaEventExternalizerConfiguration")
abstract class NotificationsModuleIntegrationTest extends ModuleIntegrationTest {

    protected static final Instant OCCURRED_AT = Instant.parse("2026-09-09T12:00:00Z");

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        SharedKafka.registerTo(registry);
    }

    @Autowired
    protected NotificationStore store;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private KafkaListenerEndpointRegistry listeners;

    /**
     * Hold every test until the consumer owns all partitions — publishing into a
     * still-rebalancing group is the main source of flakiness here.
     */
    @BeforeEach
    void awaitConsumerAssignment() {
        var container = listeners.getListenerContainer(SocialEventConsumer.LISTENER_ID);
        await().atMost(Duration.ofSeconds(30)).until(() -> container.getAssignedPartitions() != null
            && container.getAssignedPartitions().size() == NotificationsKafkaConsumerAutoConfiguration.PARTITIONS);
    }

    /**
     * Persist a notification directly, for the tests that are about deletion, not
     * ingestion.
     */
    protected final void givenNotification(NotificationType type, UUID recipientId, UUID actorId, UUID subjectId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> store.insertIfNew(UUID.randomUUID(),
            type.wireName(), recipientId, actorId, subjectId, OCCURRED_AT, Instant.now()));
    }

    protected final void publish(String key, String json) {
        try (var producer = new KafkaProducer<String, String>(
            Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SharedKafka.INSTANCE.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()))) {
            producer.send(new ProducerRecord<>(SocialEventConsumer.TOPIC, key, json)).get();
        } catch (Exception e) {
            throw new IllegalStateException("failed to publish to " + SocialEventConsumer.TOPIC, e);
        }
    }

    protected final List<Notification> notificationsFor(UUID recipientId) {
        return store.findAll().stream().filter(n -> n.recipientId().value().equals(recipientId)).toList();
    }

    /**
     * Every value currently on the dead-letter topic, read from the start with a
     * throwaway group.
     */
    protected final List<String> deadLetterValues() {
        var config = Map.<String, Object>of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            SharedKafka.INSTANCE.getBootstrapServers(), ConsumerConfig.GROUP_ID_CONFIG,
            "dlt-probe-" + UUID.randomUUID(), ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        var values = new ArrayList<String>();
        try (var consumer = new KafkaConsumer<String, String>(config)) {
            consumer.subscribe(List.of(SocialEventConsumer.DEAD_LETTER_TOPIC));
            long deadline = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < deadline) {
                consumer.poll(Duration.ofMillis(200)).forEach(record -> values.add(record.value()));
            }
        }
        return values;
    }
}
