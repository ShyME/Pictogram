package me.imshy.pictogram;

import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * One {@link MockOAuth2Server} standing in for Google for the whole app suite,
 * mirroring {@code SharedPostgres} / {@code SharedMinio}. Replaces the
 * per-class {@code MockOAuth2Server} fields that {@code AppTest} and
 * {@code GoogleSignInWebTest} each held — a per-class server meant a per-class
 * issuer URL, which forked the context cache (#78).
 *
 * <p>
 * Each test enqueues its callback immediately before the redirect dance
 * consumes it, so on a green run the FIFO queue is always drained. A test that
 * enqueues then fails before the token exchange would leave a stale callback
 * for the next OAuth test — the price of one shared server; the suite is
 * already red in that case.
 */
public final class SharedGoogle {

    public static final String ISSUER_ID = "google";
    public static final String CLIENT_ID = "pictogram-test";
    public static final String CLIENT_SECRET = "pictogram-test-secret";

    public static final MockOAuth2Server INSTANCE = new MockOAuth2Server();

    static {
        INSTANCE.start();
    }

    private SharedGoogle() {
    }

    public static void registerTo(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> CLIENT_ID);
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> CLIENT_SECRET);
        registry.add("spring.security.oauth2.client.registration.google.scope", () -> "openid,email");
        registry.add("spring.security.oauth2.client.provider.google.issuer-uri",
            () -> INSTANCE.issuerUrl(ISSUER_ID).toString());
    }
}
