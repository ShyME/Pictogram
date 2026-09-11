package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import me.imshy.chat.UserId;
import me.imshy.chat.auth.TestAccessTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

// A connection lives far longer than the 15m access token it was handshaked with
// (ChatHandshakeFilter checks the token once, at the upgrade). Left unaddressed it would keep
// delivering for hours on an expired token (#192).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketHandlerExpiryTest {

    private static final TestAccessTokens TOKENS = new TestAccessTokens();

    @DynamicPropertySource
    static void publicKey(DynamicPropertyRegistry registry) {
        registry.add("pictogram.auth.public-key", TOKENS::publicJwkJson);
    }

    @LocalServerPort
    private int port;

    private final Clock clock = Clock.systemUTC();
    private final ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
    private final List<Disposable> connections = new ArrayList<>();

    @AfterEach
    void closeConnections() {
        connections.forEach(Disposable::dispose);
    }

    @Test
    void aConnectionIsClosedWithTheDedicatedCodeWhenItsAccessTokenExpires() throws InterruptedException {
        // Comfortably longer than a first-request-to-a-fresh-context warmup (JIT, lazy codec
        // setup), so a slow handshake never masquerades as an already-expired token.
        String expiringSoon = TOKENS.issueExpiringIn(UserId.random(), clock, Duration.ofSeconds(2));
        AtomicReference<CloseStatus> closedWith = new AtomicReference<>();

        connect(expiringSoon, session -> {
            session.closeStatus().doOnNext(closedWith::set).subscribe();
            return session.receive().then();
        });

        await(() -> closedWith.get() != null);
        assertThat(closedWith.get().getCode()).isEqualTo(ChatWebSocketHandler.ACCESS_TOKEN_EXPIRED_CLOSE_CODE);
    }

    @Test
    void aConnectionWellInsideItsTokensLifetimeIsNotClosed() throws InterruptedException {
        String freshToken = TOKENS.issue(UserId.random(), clock);
        AtomicReference<CloseStatus> closedWith = new AtomicReference<>();

        connect(freshToken, session -> {
            session.closeStatus().doOnNext(closedWith::set).subscribe();
            return session.receive().then();
        });

        Thread.sleep(500);
        assertThat(closedWith.get()).isNull();
    }

    private void connect(String token, Function<WebSocketSession, Mono<Void>> body) {
        connections.add(client.execute(wsUri(), new HttpHeaders(), offering(token, body)).subscribe());
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        for (int waited = 0; waited < 100 && !condition.getAsBoolean(); waited++)
            Thread.sleep(100);
    }

    private static WebSocketHandler offering(String token, Function<WebSocketSession, Mono<Void>> body) {
        return new WebSocketHandler() {
            @Override
            public List<String> getSubProtocols() {
                return List.of(ChatWebSocketHandler.SUBPROTOCOL, token);
            }

            @Override
            public Mono<Void> handle(WebSocketSession session) {
                return body.apply(session);
            }
        };
    }

    private URI wsUri() {
        return URI.create("ws://localhost:" + port + ChatWebSocketConfiguration.WS_PATH);
    }
}
