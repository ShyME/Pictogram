package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import me.imshy.chat.UserId;
import me.imshy.chat.auth.TestAccessTokens;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;

// A real EC key pair, not the fixed dev one from application.yml — the test configures
// chat's public-key property to this pair's public half so it can sign tokens the running
// context will actually accept, and forge ones it must reject (#164).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketHandshakeTest {

    private static final TestAccessTokens TOKENS = new TestAccessTokens();

    @DynamicPropertySource
    static void publicKey(DynamicPropertyRegistry registry) {
        registry.add("pictogram.auth.public-key", TOKENS::publicJwkJson);
    }

    @LocalServerPort
    private int port;

    private final Clock clock = Clock.systemUTC();
    private final ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

    @Test
    void aValidAccessTokenIsAcceptedAtTheHandshake() {
        String token = TOKENS.issue(UserId.random(), clock);
        var negotiated = new AtomicReference<String>();

        client.execute(wsUri(), new HttpHeaders(), offering(token, session -> {
            negotiated.set(session.getHandshakeInfo().getSubProtocol());
            return session.close();
        })).block();

        // A browser (and this client) only connects if the server echoes an offered
        // subprotocol — it must be the fixed "pictogram-chat", never the token (#166).
        assertThat(negotiated).hasValue(ChatWebSocketHandler.SUBPROTOCOL);
    }

    @Test
    void aTamperedAccessTokenIsRejectedAtTheHandshake() {
        String tampered = TOKENS.issueSignedByAnotherKey(UserId.random(), clock);

        assertThatThrownBy(
            () -> client.execute(wsUri(), new HttpHeaders(), offering(tampered, WebSocketSession::close)).block())
            .isInstanceOfAny(CompletionException.class, RuntimeException.class);
    }

    @Test
    void aMissingAccessTokenIsRejectedAtTheHandshake() {
        assertThatThrownBy(() -> client.execute(wsUri(), new HttpHeaders(), WebSocketSession::close).block())
            .isInstanceOfAny(CompletionException.class, RuntimeException.class);
    }

    // Mirrors the browser client: the fixed subprotocol and the token offered
    // together,
    // the token riding as a Sec-WebSocket-Protocol value (ADR-0014).
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
