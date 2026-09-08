package me.imshy.chat.ws;

import java.util.List;
import java.util.stream.StreamSupport;
import me.imshy.chat.UserId;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// A fresh instance per handshake (built in ChatWebSocketConfiguration), each closing over
// its own verified caller — ChatMessagingTest pins that a sender's identity is per-connection.
class ChatWebSocketHandler implements WebSocketHandler {

    // A browser drops the socket unless the server echoes back a subprotocol the
    // client
    // offered, and the opaque token can't be echoed — so the client offers this
    // fixed value
    // alongside the token (frontend chatConnection.ts) and getSubProtocols()
    // selects it.
    static final String SUBPROTOCOL = "pictogram-chat";

    // Bounds one connection's outbound buffer so a stalled recipient is terminated
    // rather
    // than buffered into the heap (#174). ChatWebSocketHandlerBufferTest pins the
    // value.
    static final int OUTBOUND_BUFFER_CAPACITY = 256;

    // A batch presence query (#202) is answered for at most this many subjects; a
    // longer
    // "userIds" array is truncated rather than walked in full. Well under
    // OUTBOUND_BUFFER_CAPACITY on purpose: answerPresenceQuery emits every answer
    // synchronously into the outbound sink, so a batch near the buffer size would
    // overflow
    // it mid-loop and drop the connection. The frontend chunks longer follow lists
    // into
    // several frames (chatConnection.ts). Still client-driven and within ADR-0014's
    // intent
    // — the client asks, chat never emits unprompted.
    // ChatWebSocketHandlerPresenceQueryTest pins both the truncation and the
    // headroom.
    static final int MAX_PRESENCE_QUERY_SUBJECTS = 128;

    private final ConnectionRegistry connections;
    private final ObjectMapper json;
    private final UserId caller;

    ChatWebSocketHandler(ConnectionRegistry connections, ObjectMapper json, UserId caller) {
        this.connections = connections;
        this.json = json;
        this.caller = caller;
    }

    @Override
    public List<String> getSubProtocols() {
        return List.of(SUBPROTOCOL);
    }

    static Sinks.Many<OutboundEvent> outboundSink() {
        return Sinks.many().unicast().onBackpressureBuffer(Queues.<OutboundEvent>get(OUTBOUND_BUFFER_CAPACITY).get());
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Sinks.Many<OutboundEvent> outbound = outboundSink();
        connections.connect(caller, outbound);

        // Disconnect before completing the sink, not after: the reverse order leaves a
        // completed sink briefly in the registry, where a concurrent deliver() still
        // routes
        // to it and the message is lost.
        Mono<Void> receiving = session.receive().map(WebSocketMessage::getPayloadAsText)
            .doOnNext(payload -> handleFrame(outbound, payload)).then().doFinally(signal -> {
                connections.disconnect(caller, outbound);
                outbound.tryEmitComplete();
            });

        Mono<Void> sending = session
            .send(outbound.asFlux().map(event -> session.textMessage(json.writeValueAsString(event))));

        return receiving.and(sending);
    }

    // Malformed frames drop silently rather than tearing the connection down
    // (ChatMessagingTest). A presence query (ADR-0014) self-tags with "type"; a
    // send frame
    // has none, so it is the fall-through case.
    private void handleFrame(Sinks.Many<OutboundEvent> outbound, String payload) {
        JsonNode frame;
        try {
            frame = json.readTree(payload);
        } catch (RuntimeException malformed) {
            return;
        }
        if ("presence-query".equals(frame.at("/type").asString())) {
            answerPresenceQuery(outbound, presenceQuerySubjects(frame));
            return;
        }

        SendMessageRequest request;
        try {
            request = json.treeToValue(frame, SendMessageRequest.class);
        } catch (RuntimeException malformed) {
            return;
        }
        // treeToValue returns null (not an exception) for a bare `null` frame — pinned
        // by ChatMessagingTest.
        if (request == null || request.recipientUserId() == null || request.text() == null)
            return;

        boolean delivered = connections.deliver(caller, request.recipientUserId(), request.text());
        if (!delivered) {
            ConnectionRegistry.emit(outbound, UndeliveredMessage.of(request.recipientUserId(), request.text()));
        }
    }

    // A presence query names its subjects as either one "userId" (usePresence's
    // single
    // form) or a "userIds" array (the chat sidebar's batch form, #202). The array
    // is
    // truncated to MAX_PRESENCE_QUERY_SUBJECTS so an over-long request is never
    // walked in
    // full.
    static List<String> presenceQuerySubjects(JsonNode frame) {
        JsonNode userIds = frame.at("/userIds");
        if (!userIds.isArray())
            return List.of(frame.at("/userId").asString());
        return StreamSupport.stream(userIds.spliterator(), false).limit(MAX_PRESENCE_QUERY_SUBJECTS)
            .map(JsonNode::asString).toList();
    }

    // One PresenceStatus per subject; an entry that is not a UserId is skipped, not
    // fatal
    // to the rest of the frame (matches the original single-subject behaviour).
    private void answerPresenceQuery(Sinks.Many<OutboundEvent> outbound, List<String> subjectUserIds) {
        for (String userId : subjectUserIds) {
            UserId subject;
            try {
                subject = UserId.fromString(userId);
            } catch (RuntimeException notAUserId) {
                continue;
            }
            ConnectionRegistry.emit(outbound, PresenceStatus.of(subject, connections.isOnline(subject)));
        }
    }
}
