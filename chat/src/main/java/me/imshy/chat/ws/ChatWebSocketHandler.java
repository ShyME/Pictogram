package me.imshy.chat.ws;

import me.imshy.chat.UserId;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

// One instance per connection (#165) — ChatWebSocketConfiguration builds a fresh one per
// handshake, closing over the caller ChatHandshakeFilter already verified.
class ChatWebSocketHandler implements WebSocketHandler {

    private final ConnectionRegistry connections;
    private final ObjectMapper json;
    private final UserId caller;

    ChatWebSocketHandler(ConnectionRegistry connections, ObjectMapper json, UserId caller) {
        this.connections = connections;
        this.json = json;
        this.caller = caller;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Sinks.Many<OutboundEvent> outbound = Sinks.many().unicast().onBackpressureBuffer();
        connections.connect(caller, outbound);

        // Disconnect before completing the sink, not after: otherwise a concurrent
        // deliver() can still find this (already-terminated) sink in the registry and
        // silently lose a message to it in the window between the two.
        Mono<Void> receiving = session.receive().map(WebSocketMessage::getPayloadAsText)
            .doOnNext(payload -> relay(outbound, payload)).then().doFinally(signal -> {
                connections.disconnect(caller, outbound);
                outbound.tryEmitComplete();
            });

        Mono<Void> sending = session
            .send(outbound.asFlux().map(event -> session.textMessage(json.writeValueAsString(event))));

        return receiving.and(sending);
    }

    // A malformed frame (bad JSON, a non-UUID recipientUserId, or none at all)
    // drops
    // silently rather than tearing down the whole connection over one bad message.
    private void relay(Sinks.Many<OutboundEvent> outbound, String payload) {
        SendMessageRequest request;
        try {
            request = json.readValue(payload, SendMessageRequest.class);
        } catch (RuntimeException malformed) {
            return;
        }
        if (request.recipientUserId() == null || request.text() == null)
            return;

        boolean delivered = connections.deliver(caller, request.recipientUserId(), request.text());
        if (!delivered) {
            outbound.emitNext(UndeliveredMessage.of(request.recipientUserId(), request.text()),
                ConnectionRegistry.emitRetrying());
        }
    }
}
