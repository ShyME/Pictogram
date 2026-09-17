package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import reactor.core.publisher.Sinks;
import tools.jackson.databind.json.JsonMapper;

// #192 bounds inbound frame *size* (ChatWebSocketHandlerInboundFrameLimitTest); nothing bounded
// frame *frequency* — a connected client could send as fast as the transport allowed (#249).
// Mirrors that test's treatment: exceeding the limit closes the connection with its own
// dedicated code rather than throttling silently.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketHandlerRateLimitTest {

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
    void aBurstPastTheLimitClosesTheConnectionWithTheDedicatedCode() throws InterruptedException {
        Connection connection = connect();

        for (int frame = 0; frame < InboundFrameRateLimiter.CAPACITY + 5; frame++)
            connection.send(UserId.random(), "hi");

        assertThat(connection.closed.await(5, TimeUnit.SECONDS))
                .as("connection closed within 5s")
                .isTrue();
        assertThat(connection.closedWith.get().getCode())
                .isEqualTo(ChatWebSocketHandler.RATE_LIMIT_EXCEEDED_CLOSE_CODE);
    }

    @Test
    void aBurstWithinTheLimitLeavesTheConnectionOpen() throws InterruptedException {
        Connection connection = connect();

        for (int frame = 0; frame < InboundFrameRateLimiter.CAPACITY; frame++) connection.send(UserId.random(), "hi");

        assertThat(connection.closed.await(500, TimeUnit.MILLISECONDS))
                .as("still open past the deadline")
                .isFalse();
    }

    private Connection connect() throws InterruptedException {
        Connection connection = new Connection(client, wsUri(), TOKENS.issue(UserId.random(), clock));
        connections.add(connection);
        assertThat(connection.ready.await(5, TimeUnit.SECONDS))
                .as("connection opened within 5s")
                .isTrue();
        return connection;
    }

    private URI wsUri() {
        return URI.create("ws://localhost:" + port + ChatWebSocketConfiguration.WS_PATH);
    }

    private static final class Connection {

        private final CountDownLatch ready = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final AtomicReference<CloseStatus> closedWith = new AtomicReference<>();
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
                            session.closeStatus()
                                    .doOnNext(status -> {
                                        closedWith.set(status);
                                        closed.countDown();
                                    })
                                    .subscribe();
                            Mono<Void> receiving = session.receive().then();
                            Mono<Void> sending = session.send(outbound.asFlux().map(session::textMessage));
                            return receiving.and(sending);
                        }
                    })
                    .subscribe();
        }

        void send(UserId recipient, String text) {
            outbound.emitNext(
                    JSON.writeValueAsString(new SendMessageRequest(recipient, text)),
                    Sinks.EmitFailureHandler.FAIL_FAST);
        }

        void close() {
            subscription.dispose();
        }
    }
}
