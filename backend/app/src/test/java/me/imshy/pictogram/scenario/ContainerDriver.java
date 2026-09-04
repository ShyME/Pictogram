package me.imshy.pictogram.scenario;

import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import me.imshy.pictogram.scenario.PictogramApi.Actor;
import tools.jackson.databind.ObjectMapper;

/**
 * Blackbox transport for the {@code *Scenarios} (#20). Everything about speaking the API lives in
 * {@link HttpPictogramApi} (#51); this class only supplies what differs from the in-process run — a
 * base URI from the running stack, an {@link InteractiveLoginSignIn}, and a {@link
 * NamespacedUsernamePolicy} — plus one thing the in-process run gets for free: isolation.
 *
 * <p>The container's Postgres is never truncated, so the scenarios' hard-coded emails and usernames
 * ({@code "ada@example.com"}, {@code "ada_lovelace"}) would collide across methods. Each instance
 * (one per test — {@code @BeforeEach} rebuilds it) carries a {@link #namespace} token: it prefixes
 * every email itself and hands the token to the username policy, so a scenario body reads the handle
 * it wrote and nothing leaks between tests (#154).
 */
final class ContainerDriver implements PictogramApi {

    // :app:test runs one fork (Gradle default), so the per-JVM sequence alone makes the token
    // unique across a run; the random suffix only matters the day someone sets maxParallelForks > 1.
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final PictogramApi http;
    private final String namespace;

    ContainerDriver(URI baseUri, ObjectMapper json) {
        this.namespace = "bb%s%03d"
                .formatted(
                        Long.toString(SEQUENCE.incrementAndGet(), 36),
                        ThreadLocalRandom.current().nextInt(1000));
        this.http = new HttpPictogramApi(
                baseUri, json, new InteractiveLoginSignIn(baseUri), new NamespacedUsernamePolicy(namespace));
    }

    @Override
    public Actor registerViaGoogle(String email) {
        return http.registerViaGoogle(namespace + email);
    }
}
