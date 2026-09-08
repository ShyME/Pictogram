package me.imshy.pictogram.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The one Kafka broker for the whole suite — a JVM-wide singleton it owns the
 * lifecycle of (started in the static block, stopped only at JVM exit),
 * mirroring {@link SharedPostgres}.
 *
 * <p>
 * Only the {@code notifications} module consumes {@code pictogram.social}
 * (ADR-0015), so it is the only module whose tests
 * {@link #registerTo(DynamicPropertyRegistry) point at} this container. Every
 * other slice excludes {@code KafkaAutoConfiguration} and starts with no broker
 * — see {@code application-test.yml}. Unused until ticket #197.
 *
 * <p>
 * The image is the JVM {@code apache/kafka}, not the
 * {@code apache/kafka-native} that compose runs: Testcontainers'
 * {@code KafkaContainer} drives the {@code apache/kafka} layout, and a test JVM
 * carries none of the deployment box's memory pressure.
 */
public final class SharedKafka {

    private static final boolean REUSE = System.getenv("CI") == null;

    public static final KafkaContainer INSTANCE = new KafkaContainer(DockerImageName.parse("apache/kafka:4.2.1"))
        .withReuse(REUSE);

    static {
        INSTANCE.start();
    }

    private SharedKafka() {
    }

    public static void registerTo(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", INSTANCE::getBootstrapServers);
    }
}
