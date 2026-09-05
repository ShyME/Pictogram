package me.imshy.pictogram.scenario;

import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import tools.jackson.databind.ObjectMapper;

/**
 * <p>
 * The container's Postgres is never truncated, so the scenarios' hard-coded
 * emails and usernames ({@code "ada@example.com"}, {@code "ada_lovelace"})
 * would collide across methods. Each instance (one per test —
 * {@code @BeforeEach} rebuilds it) carries a {@link #namespace} token: it
 * prefixes every email itself and hands the token to the username policy, so a
 * scenario body reads the handle it wrote and nothing leaks between tests
 * (#154).
 */
final class RunningStackApp implements PictogramApp {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final PictogramApp http;
    private final String namespace;

    RunningStackApp(URI baseUri, ObjectMapper json) {
        this.namespace = "bb%s%03d".formatted(Long.toString(SEQUENCE.incrementAndGet(), 36),
            ThreadLocalRandom.current().nextInt(1000));
        this.http = new HttpPictogramApp(baseUri, json, new InteractiveLoginSignIn(baseUri),
            new NamespacedUsernameStrategy(namespace));
    }

    @Override
    public PictogramApi registerViaGoogle(String email) {
        return http.registerViaGoogle(namespace + email);
    }
}
