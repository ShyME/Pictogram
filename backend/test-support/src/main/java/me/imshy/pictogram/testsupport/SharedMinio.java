package me.imshy.pictogram.testsupport;

import java.time.Duration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class SharedMinio {

    private static final int API_PORT = 9000;
    private static final String ACCESS_KEY = "pictogram-test";
    private static final String SECRET_KEY = "pictogram-test-secret";
    private static final String BUCKET = "pictogram-media-test";

    public static final GenericContainer<?> INSTANCE =
            new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z"))
                    .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
                    .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
                    .withCommand("server", "/data")
                    .withExposedPorts(API_PORT)
                    .waitingFor(Wait.forHttp("/minio/health/ready").forPort(API_PORT)
                            .withStartupTimeout(Duration.ofSeconds(60)));

    static {
        INSTANCE.start();
    }

    private SharedMinio() {
    }

    public static void registerTo(DynamicPropertyRegistry registry) {
        registry.add("pictogram.media.storage.endpoint",
                () -> "http://" + INSTANCE.getHost() + ":" + INSTANCE.getMappedPort(API_PORT));
        registry.add("pictogram.media.storage.region", () -> "us-east-1");
        registry.add("pictogram.media.storage.access-key", () -> ACCESS_KEY);
        registry.add("pictogram.media.storage.secret-key", () -> SECRET_KEY);
        registry.add("pictogram.media.storage.bucket", () -> BUCKET);
        registry.add("pictogram.media.storage.path-style-access", () -> true);
    }
}
