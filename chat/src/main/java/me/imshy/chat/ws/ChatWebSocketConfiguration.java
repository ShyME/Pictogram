package me.imshy.chat.ws;

import java.time.Clock;
import java.time.Instant;
import me.imshy.chat.UserId;
import me.imshy.chat.auth.AccessTokenVerifier;
import me.imshy.chat.auth.AuthProperties;
import me.imshy.chat.auth.PublicSigningKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.WebsocketServerSpec;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class ChatWebSocketConfiguration {

    static final String WS_PATH = "/ws";

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AccessTokenVerifier accessTokenVerifier(AuthProperties properties, Clock clock) {
        return new AccessTokenVerifier(PublicSigningKey.fromJwkJson(properties.publicKey()).jwkSource(),
            properties.issuer(), clock);
    }

    @Bean
    WebFilter chatHandshakeFilter(AccessTokenVerifier verifier) {
        return new ChatHandshakeFilter(WS_PATH, verifier);
    }

    @Bean
    ConnectionRegistry connectionRegistry() {
        return new ConnectionRegistry();
    }

    @Bean
    HandlerMapping chatWebSocketMapping(ConnectionRegistry connections, ObjectMapper json, Clock clock) {
        return new ChatWebSocketHandlerMapping(connections, json, clock);
    }

    // The default HandshakeWebSocketService leaves Reactor Netty's inbound frame size at its
    // 64 KiB default; ChatWebSocketHandler.MAX_INBOUND_FRAME_PAYLOAD_LENGTH is the explicit,
    // tested bound (#192).
    @Bean
    WebSocketService chatWebSocketService() {
        return new HandshakeWebSocketService(new ReactorNettyRequestUpgradeStrategy(
            () -> WebsocketServerSpec.builder()
                .maxFramePayloadLength(ChatWebSocketHandler.MAX_INBOUND_FRAME_PAYLOAD_LENGTH)));
    }

    @Bean
    WebSocketHandlerAdapter chatWebSocketHandlerAdapter(WebSocketService chatWebSocketService) {
        return new WebSocketHandlerAdapter(chatWebSocketService);
    }

    // SimpleUrlHandlerMapping would hand out one shared handler for every request;
    // this
    // builds a fresh ChatWebSocketHandler per handshake so each closes over the
    // caller
    // ChatHandshakeFilter attributed to that exchange.
    private static final class ChatWebSocketHandlerMapping implements HandlerMapping, Ordered {

        private final ConnectionRegistry connections;
        private final ObjectMapper json;
        private final Clock clock;

        ChatWebSocketHandlerMapping(ConnectionRegistry connections, ObjectMapper json, Clock clock) {
            this.connections = connections;
            this.json = json;
            this.clock = clock;
        }

        @Override
        public Mono<Object> getHandler(ServerWebExchange exchange) {
            if (!WS_PATH.equals(exchange.getRequest().getPath().value()))
                return Mono.empty();

            UserId caller = exchange.getAttribute(ChatHandshakeFilter.USER_ID_ATTRIBUTE);
            Instant expiresAt = exchange.getAttribute(ChatHandshakeFilter.EXPIRES_AT_ATTRIBUTE);
            return Mono.just(new ChatWebSocketHandler(connections, json, caller, expiresAt, clock));
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
