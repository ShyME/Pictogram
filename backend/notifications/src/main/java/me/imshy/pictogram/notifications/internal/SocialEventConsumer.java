package me.imshy.pictogram.notifications.internal;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * The hand-written {@code spring-kafka} consumer of {@code pictogram.social}
 * (ADR-0015) — hand-written, not Modulith's incoming side, so the consumer
 * group, the manual ack and the error handling are all visible here and in
 * {@link NotificationsKafkaConsumerAutoConfiguration}.
 *
 * <p>
 * The record is deserialised, turned into a notification in its own
 * transaction, and only then acked — a crash before the commit redelivers the
 * record, and the insert-or-ignore in {@link NotificationStore} makes that
 * redelivery a no-op. A handler that throws is retried and finally
 * dead-lettered by the error handler; {@link #onDeadLetter} is the DLT's only
 * reader.
 */
@Component
class SocialEventConsumer {

    static final String TOPIC = "pictogram.social";
    static final String DEAD_LETTER_TOPIC = "pictogram.social.DLT";
    static final String GROUP = "notifications";
    static final String LISTENER_ID = "pictogram-social";

    private static final Logger log = LoggerFactory.getLogger(SocialEventConsumer.class);

    // A plain mapper, mirroring the producer: Modulith's Kafka externaliser
    // serialises the
    // SocialEvent with a bare JsonMapper, and the wire is only @JsonValue id
    // strings, an
    // ISO-8601 instant and a discriminator — nothing that needs app-wide Jackson
    // config.
    private final JsonMapper json = JsonMapper.builder().build();

    private final Notifications notifications;
    private final TransactionTemplate transaction;

    SocialEventConsumer(Notifications notifications, PlatformTransactionManager transactionManager) {
        this.notifications = notifications;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @KafkaListener(id = LISTENER_ID, topics = TOPIC, groupId = GROUP,
        containerFactory = NotificationsKafkaConsumerAutoConfiguration.CONTAINER_FACTORY)
    void onSocialEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        SocialEventMessage event = json.readValue(record.value(), SocialEventMessage.class);
        transaction.executeWithoutResult(status -> notifications.record(event));
        ack.acknowledge();
    }

    @KafkaListener(topics = DEAD_LETTER_TOPIC, groupId = GROUP + ".dlt",
        containerFactory = NotificationsKafkaConsumerAutoConfiguration.DEAD_LETTER_CONTAINER_FACTORY)
    void onDeadLetter(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.error("pictogram.social record dead-lettered after retries: key={}, value={}", record.key(),
            record.value());
        ack.acknowledge();
    }
}
