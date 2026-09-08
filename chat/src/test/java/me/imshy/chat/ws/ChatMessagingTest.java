package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
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
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.json.JsonMapper;

// End to end within chat itself (#165): a message sent over one connection reaches every
// open connection the recipient currently has, or reports undelivered back to the sender
// when the recipient has none.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatMessagingTest {

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
    void aMessageIsDeliveredToEveryOpenConnectionTheRecipientHas() throws InterruptedException {
        UserId sender = UserId.random();
        UserId recipient = UserId.random();
        Connection firstTab = connect(recipient);
        Connection secondTab = connect(recipient);

        connect(sender).send(new SendMessageRequest(recipient, "hello"));

        assertThat(receive(firstTab, DeliveredMessage.class)).isEqualTo(DeliveredMessage.of(sender, "hello"));
        assertThat(receive(secondTab, DeliveredMessage.class)).isEqualTo(DeliveredMessage.of(sender, "hello"));
    }

    @Test
    void aMessageToAUserWithNoOpenConnectionIsReportedUndeliveredOnTheSendersConnection() throws InterruptedException {
        UserId sender = UserId.random();
        UserId recipientWithNoConnection = UserId.random();
        Connection senderConnection = connect(sender);

        senderConnection.send(new SendMessageRequest(recipientWithNoConnection, "hello"));

        assertThat(receive(senderConnection, UndeliveredMessage.class))
            .isEqualTo(UndeliveredMessage.of(recipientWithNoConnection, "hello"));
    }

    @Test
    void sendingIsUnrestrictedBetweenTwoUsersWithNoPriorRelationship() throws InterruptedException {
        UserId sender = UserId.random();
        UserId recipient = UserId.random();
        Connection recipientConnection = connect(recipient);

        connect(sender).send(new SendMessageRequest(recipient, "hi, stranger"));

        assertThat(receive(recipientConnection, DeliveredMessage.class))
            .isEqualTo(DeliveredMessage.of(sender, "hi, stranger"));
    }

    @Test
    void aPresenceQueryReportsOnlineForAConnectedUserAndOfflineForOneWithNoConnection() throws InterruptedException {
        UserId connectedUser = UserId.random();
        UserId userWithNoConnection = UserId.random();
        connect(connectedUser);
        Connection asker = connect(UserId.random());

        asker.sendRaw(JSON.writeValueAsString(Map.of("type", "presence-query", "userId", connectedUser)));
        assertThat(receive(asker, PresenceStatus.class)).isEqualTo(PresenceStatus.of(connectedUser, true));

        asker.sendRaw(JSON.writeValueAsString(Map.of("type", "presence-query", "userId", userWithNoConnection)));
        assertThat(receive(asker, PresenceStatus.class)).isEqualTo(PresenceStatus.of(userWithNoConnection, false));
    }

    @Test
    void aMalformedFrameIsDroppedRatherThanTearingDownTheConnection() throws InterruptedException {
        UserId sender = UserId.random();
        UserId recipient = UserId.random();
        Connection recipientConnection = connect(recipient);
        Connection senderConnection = connect(sender);

        senderConnection.sendRaw("{\"text\":\"missing a recipient\"}");
        senderConnection.sendRaw("{\"recipientUserId\":\"not-a-uuid\",\"text\":\"bad id\"}");
        senderConnection.sendRaw("not even json");
        senderConnection.sendRaw("null");
        senderConnection.send(new SendMessageRequest(recipient, "still works"));

        assertThat(receive(recipientConnection, DeliveredMessage.class))
            .isEqualTo(DeliveredMessage.of(sender, "still works"));
    }

    private <T> T receive(Connection connection, Class<T> type) throws InterruptedException {
        String payload = connection.inbound.poll(5, TimeUnit.SECONDS);
        assertThat(payload).as("expected a message within 5s").isNotNull();
        return JSON.readValue(payload, type);
    }

    private Connection connect(UserId user) throws InterruptedException {
        Connection connection = new Connection(client, wsUri(), TOKENS.issue(user, clock));
        connections.add(connection);
        assertThat(connection.ready.await(5, TimeUnit.SECONDS)).as("connection opened within 5s").isTrue();
        return connection;
    }

    private URI wsUri() {
        return URI.create("ws://localhost:" + port + ChatWebSocketConfiguration.WS_PATH);
    }

    // Holds one client-side connection open in the background for the test to
    // drive:
    // `ready` fires once the handshake completes (by which point the server has
    // already
    // registered the connection, since ChatWebSocketHandler.handle registers
    // synchronously
    // before the upgrade response reaches this client), `send` pushes a request
    // frame, and
    // `inbound` collects every frame the server sends back.
    private static final class Connection {

        private final BlockingQueue<String> inbound = new LinkedBlockingQueue<>();
        private final CountDownLatch ready = new CountDownLatch(1);
        private final Sinks.Many<String> outbound = Sinks.many().unicast().onBackpressureBuffer();
        private final Sinks.Empty<Void> closeSignal = Sinks.empty();
        private final Disposable subscription;

        // Offers the fixed subprotocol and the token together, as the browser client
        // does
        // (frontend chatConnection.ts) — the token rides as a Sec-WebSocket-Protocol
        // value
        // (ADR-0014) and the server echoes back the fixed one.
        Connection(ReactorNettyWebSocketClient client, URI uri, String token) {
            subscription = client.execute(uri, new HttpHeaders(), new WebSocketHandler() {
                @Override
                public List<String> getSubProtocols() {
                    return List.of(ChatWebSocketHandler.SUBPROTOCOL, token);
                }

                @Override
                public Mono<Void> handle(WebSocketSession session) {
                    ready.countDown();
                    Mono<Void> receiving = session.receive().map(WebSocketMessage::getPayloadAsText)
                        .doOnNext(inbound::add).then();
                    Mono<Void> sending = session.send(outbound.asFlux().map(session::textMessage));
                    return Mono.when(receiving, sending, closeSignal.asMono());
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
            closeSignal.tryEmitEmpty();
            subscription.dispose();
        }
    }
}
