package me.imshy.chat.ws;

import java.util.List;
import me.imshy.chat.UserId;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// One instance per connection (#165) — ChatWebSocketConfiguration builds a fresh one per
// handshake, closing over the caller ChatHandshakeFilter already verified.
class ChatWebSocketHandler implements WebSocketHandler {

    // The browser drops the socket unless the server echoes a subprotocol it
    // offered,
    // and the opaque token can't be echoed — so the client also offers this fixed
    // value (frontend chatConnection.ts) and getSubProtocols() selects it.
    static final String SUBPROTOCOL = "pictogram-chat";

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

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Sinks.Many<OutboundEvent> outbound = Sinks.many().unicast().onBackpressureBuffer();
        connections.connect(caller, outbound);

        // Disconnect before completing the sink, not after: otherwise a concurrent
        // deliver() can still find this (already-terminated) sink in the registry and
        // silently lose a message to it in the window between the two.
        Mono<Void> receiving = session.receive().map(WebSocketMessage::getPayloadAsText)
            .doOnNext(payload -> handleFrame(outbound, payload)).then().doFinally(signal -> {
                connections.disconnect(caller, outbound);
                outbound.tryEmitComplete();
            });

        Mono<Void> sending = session
            .send(outbound.asFlux().map(event -> session.textMessage(json.writeValueAsString(event))));

        return receiving.and(sending);
    }

    // A send frame carries no "type"; a presence query (ADR-0014) tags itself with
    // one.
    // Any malformed frame drops silently rather than tearing the connection down.
    private void handleFrame(Sinks.Many<OutboundEvent> outbound, String payload) {
        JsonNode frame;
        try {
            frame = json.readTree(payload);
        } catch (RuntimeException malformed) {
            return;
        }
        if ("presence-query".equals(frame.at("/type").asString())) {
            answerPresenceQuery(outbound, frame.at("/userId").asString());
            return;
        }

        SendMessageRequest request;
        try {
            request = json.treeToValue(frame, SendMessageRequest.class);
        } catch (RuntimeException malformed) {
            return;
        }
        // treeToValue returns null for a bare JSON `null` payload rather than throwing.
        if (request == null || request.recipientUserId() == null || request.text() == null)
            return;

        boolean delivered = connections.deliver(caller, request.recipientUserId(), request.text());
        if (!delivered) {
            outbound.emitNext(UndeliveredMessage.of(request.recipientUserId(), request.text()),
                ConnectionRegistry.emitRetrying());
        }
    }

    private void answerPresenceQuery(Sinks.Many<OutboundEvent> outbound, String userId) {
        UserId subject;
        try {
            subject = UserId.fromString(userId);
        } catch (RuntimeException notAUserId) {
            return;
        }
        outbound.emitNext(PresenceStatus.of(subject, connections.isOnline(subject)), ConnectionRegistry.emitRetrying());
    }
}
