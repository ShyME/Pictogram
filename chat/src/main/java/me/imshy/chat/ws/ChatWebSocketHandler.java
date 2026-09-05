package me.imshy.chat.ws;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * Holds a session open once {@link ChatHandshakeFilter} has attributed it to a
 * caller; routing messages by sender/recipient is #165's concern.
 */
public class ChatWebSocketHandler implements WebSocketHandler {

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.receive().then();
    }
}
