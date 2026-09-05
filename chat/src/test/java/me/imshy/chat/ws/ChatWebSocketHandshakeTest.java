package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import me.imshy.chat.UserId;
import me.imshy.chat.auth.TestAccessTokens;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

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
        var connected = new AtomicBoolean(false);

        client.execute(wsUri(), protocolHeader(token), session -> {
            connected.set(true);
            return session.close();
        }).block();

        assertThat(connected).isTrue();
    }

    @Test
    void aTamperedAccessTokenIsRejectedAtTheHandshake() {
        String tampered = TOKENS.issueSignedByAnotherKey(UserId.random(), clock);

        assertThatThrownBy(() -> client.execute(wsUri(), protocolHeader(tampered), noOpHandler()).block())
            .isInstanceOfAny(CompletionException.class, RuntimeException.class);
    }

    @Test
    void aMissingAccessTokenIsRejectedAtTheHandshake() {
        assertThatThrownBy(() -> client.execute(wsUri(), new HttpHeaders(), noOpHandler()).block())
            .isInstanceOfAny(CompletionException.class, RuntimeException.class);
    }

    private static org.springframework.web.reactive.socket.WebSocketHandler noOpHandler() {
        return WebSocketSession::close;
    }

    private static HttpHeaders protocolHeader(String token) {
        var headers = new HttpHeaders();
        headers.add("Sec-WebSocket-Protocol", token);
        return headers;
    }

    private URI wsUri() {
        return URI.create("ws://localhost:" + port + ChatWebSocketConfiguration.WS_PATH);
    }
}
