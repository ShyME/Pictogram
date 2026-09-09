package me.imshy.pictogram.notifications.internal;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * The {@code pictogram.social} consumer's wiring, spelled out here rather than
 * left to {@code spring.kafka.*} properties so the consumer group, the offset
 * reset, the manual ack and the retry / dead-letter policy are all visible in
 * one place (ADR-0015). Only {@code spring.kafka.bootstrap-servers} is taken
 * from configuration.
 *
 * <p>
 * An auto-configuration so the shared test profile can switch the whole
 * consumer off by name ({@code application-test.yml}) the same way it switches
 * off {@code KafkaAutoConfiguration} — every module slice starts broker-free
 * except this module's own {@code @ApplicationModuleTest}, which re-includes
 * it.
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnBean(KafkaOperations.class)
public class NotificationsKafkaConsumerAutoConfiguration {

    static final String CONTAINER_FACTORY = "socialEventListenerContainerFactory";
    static final String DEAD_LETTER_CONTAINER_FACTORY = "deadLetterListenerContainerFactory";
    static final int PARTITIONS = 3;
    static final int CONCURRENCY = 3;

    /**
     * The dead-letter producer's factory. Deliberately not a bean: a
     * {@code KafkaTemplate} or {@code ProducerFactory} bean would satisfy
     * {@code KafkaAutoConfiguration}'s {@code @ConditionalOnMissingBean} and leave
     * the Modulith relay without the app's ByteArray-serialising template. Closed
     * in {@link #closeDeadLetterProducers()}.
     */
    private DefaultKafkaProducerFactory<String, String> deadLetterProducers;

    /**
     * Both topics provisioned with {@link #PARTITIONS} partitions so
     * {@link #CONCURRENCY} consumer threads each own one and one recipient's
     * notifications stay ordered on a single partition. Matches the broker's
     * {@code KAFKA_NUM_PARTITIONS} in compose; declared here so a broker with topic
     * auto-creation off still works.
     */
    @Bean
    NewTopic pictogramSocialTopic() {
        return TopicBuilder.name(SocialEventConsumer.TOPIC).partitions(PARTITIONS).build();
    }

    @Bean
    NewTopic pictogramSocialDeadLetterTopic() {
        return TopicBuilder.name(SocialEventConsumer.DEAD_LETTER_TOPIC).partitions(PARTITIONS).build();
    }

    // This is the app's only Kafka consumer (ADR-0015), so being the sole
    // ConsumerFactory bean — which suppresses Boot's default — is harmless.
    @Bean
    ConsumerFactory<String, String> socialEventConsumerFactory(KafkaProperties properties) {
        Map<String, Object> config = properties.buildConsumerProperties();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, SocialEventConsumer.GROUP);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // The relay side (spring-modulith-events-kafka) makes ByteArray the app-wide
        // default; this consumer reads plain JSON strings.
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean(CONTAINER_FACTORY)
    ConcurrentKafkaListenerContainerFactory<String, String> socialEventListenerContainerFactory(
        ConsumerFactory<String, String> socialEventConsumerFactory, DefaultErrorHandler socialEventErrorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(socialEventConsumerFactory);
        factory.setConcurrency(CONCURRENCY);
        factory.setCommonErrorHandler(socialEventErrorHandler);
        // The handler acks each record once its own write transaction has committed.
        factory.getContainerProperties().setAckMode(AckMode.MANUAL);
        return factory;
    }

    /**
     * The DLT listener runs on its own factory with the default error handler —
     * never {@link #socialEventErrorHandler}, whose recoverer would re-publish a
     * failed log record straight back onto the DLT it was just read from.
     */
    @Bean(DEAD_LETTER_CONTAINER_FACTORY)
    ConcurrentKafkaListenerContainerFactory<String, String> deadLetterListenerContainerFactory(
        ConsumerFactory<String, String> socialEventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(socialEventConsumerFactory);
        factory.getContainerProperties().setAckMode(AckMode.MANUAL);
        return factory;
    }

    @Bean
    DefaultErrorHandler socialEventErrorHandler(KafkaProperties properties) {
        // A dedicated String-serialising producer. The app's shared KafkaTemplate is a
        // ByteArraySerializer one (spring-modulith-events-kafka, for the relay); these
        // dead-lettered records carry String values.
        Map<String, Object> config = properties.buildProducerProperties();
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        this.deadLetterProducers = new DefaultKafkaProducerFactory<>(config);

        // Partition -1 lets the partitioner place the dead-lettered record: the DLT's
        // partition count need not match the source topic's, so the source partition
        // index cannot be copied across.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            new KafkaTemplate<>(this.deadLetterProducers),
            (record, exception) -> new TopicPartition(SocialEventConsumer.DEAD_LETTER_TOPIC, -1));

        // 3 retry attempts after the first delivery (backoff 1s, 2s, 4s), then
        // dead-letter the record and commit the offset — a poison record blocks its
        // partition only for the backoff, never for good (ADR-0015).
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);

        return new DefaultErrorHandler(recoverer, backOff);
    }

    @PreDestroy
    void closeDeadLetterProducers() {
        if (this.deadLetterProducers != null) {
            this.deadLetterProducers.destroy();
        }
    }
}
