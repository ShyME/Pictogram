package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import me.imshy.chat.UserId;
import me.imshy.chat.auth.TestAccessTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.json.JsonMapper;

// The inbound path (ChatWebSocketHandler.handle's session.receive()) relied on Reactor
// Netty's default max frame length — never set explicitly, never tested — a cheap
// heap-pressure vector on a service that otherwise bounds its outbound buffer
// (ChatWebSocketHandlerBufferTest). Mirrors that test's treatment: an over-limit frame
// closes the connection rather than being buffered (#192).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketHandlerInboundFrameLimitTest {

    private static final TestAccessTokens TOKENS = new TestAccessTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @DynamicPropertySource
    static void publicKey(DynamicPropertyRegistry registry) {
        registry.add("pictogram.auth.public-key", TOKENS::publicJwkJson);
    }

    @LocalServerPort
    private int port;

    private final Clock clock = Clock.systemUTC();
    private final ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
    private final List<Connection> connections = new ArrayList<>();

    @AfterEach
    void closeConnections() {
        connections.forEach(Connection::close);
    }

    @Test
    void anOversizedInboundFrameClosesTheConnectionRatherThanBeingBuffered() throws InterruptedException {
        String tooLong = "x".repeat(ChatWebSocketHandler.MAX_INBOUND_FRAME_PAYLOAD_LENGTH + 1);
        Connection connection = connect();

        connection.sendRaw(tooLong);

        assertThat(connection.closed.await(5, TimeUnit.SECONDS)).as("connection closed within 5s").isTrue();
    }

    @Test
    void aFrameAtTheLimitIsAcceptedRatherThanClosingTheConnection() throws InterruptedException {
        UserId recipient = UserId.random();
        // Well below the limit, not right at it: the frame is the JSON envelope around this
        // text, and SendMessageRequest's field names and the recipient's UUID add their own
        // bytes on top.
        String atLimit = "x".repeat(ChatWebSocketHandler.MAX_INBOUND_FRAME_PAYLOAD_LENGTH - 200);
        Connection connection = connect();

        connection.send(new SendMessageRequest(recipient, atLimit));

        assertThat(connection.closed.await(500, TimeUnit.MILLISECONDS)).as("still open past the deadline").isFalse();
    }

    private Connection connect() throws InterruptedException {
        Connection connection = new Connection(client, wsUri(), TOKENS.issue(UserId.random(), clock));
        connections.add(connection);
        assertThat(connection.ready.await(5, TimeUnit.SECONDS)).as("connection opened within 5s").isTrue();
        return connection;
    }

    private URI wsUri() {
        return URI.create("ws://localhost:" + port + ChatWebSocketConfiguration.WS_PATH);
    }

    private static final class Connection {

        private final CountDownLatch ready = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final Sinks.Many<String> outbound = Sinks.many().unicast().onBackpressureBuffer();
        private final Disposable subscription;

        Connection(ReactorNettyWebSocketClient client, URI uri, String token) {
            subscription = client.execute(uri, new HttpHeaders(), new WebSocketHandler() {
                @Override
                public List<String> getSubProtocols() {
                    return List.of(ChatWebSocketHandler.SUBPROTOCOL, token);
                }

                @Override
                public Mono<Void> handle(WebSocketSession session) {
                    ready.countDown();
                    session.closeStatus().doOnNext(status -> closed.countDown()).subscribe();
                    Mono<Void> receiving = session.receive().then();
                    Mono<Void> sending = session.send(outbound.asFlux().map(session::textMessage));
                    return receiving.and(sending);
                }
            }).subscribe();
        }

        void send(SendMessageRequest request) {
            sendRaw(JSON.writeValueAsString(request));
        }

        void sendRaw(String payload) {
            outbound.emitNext(payload, Sinks.EmitFailureHandler.FAIL_FAST);
        }

        void close() {
            subscription.dispose();
        }
    }
}
