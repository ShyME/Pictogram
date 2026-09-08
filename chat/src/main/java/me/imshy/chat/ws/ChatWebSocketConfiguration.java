package me.imshy.chat.ws;

import java.time.Clock;
import me.imshy.chat.UserId;
import me.imshy.chat.auth.AccessTokenVerifier;
import me.imshy.chat.auth.AuthProperties;
import me.imshy.chat.auth.PublicSigningKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
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
    HandlerMapping chatWebSocketMapping(ConnectionRegistry connections, ObjectMapper json) {
        return new ChatWebSocketHandlerMapping(connections, json);
    }

    @Bean
    WebSocketHandlerAdapter chatWebSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    // SimpleUrlHandlerMapping would hand out one shared handler for every request;
    // this
    // builds a fresh ChatWebSocketHandler per handshake so each closes over the
    // caller
    // ChatHandshakeFilter attributed to that exchange.
    private static final class ChatWebSocketHandlerMapping implements HandlerMapping, Ordered {

        private final ConnectionRegistry connections;
        private final ObjectMapper json;

        ChatWebSocketHandlerMapping(ConnectionRegistry connections, ObjectMapper json) {
            this.connections = connections;
            this.json = json;
        }

        @Override
        public Mono<Object> getHandler(ServerWebExchange exchange) {
            if (!WS_PATH.equals(exchange.getRequest().getPath().value()))
                return Mono.empty();

            UserId caller = exchange.getAttribute(ChatHandshakeFilter.USER_ID_ATTRIBUTE);
            return Mono.just(new ChatWebSocketHandler(connections, json, caller));
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
