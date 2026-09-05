package me.imshy.chat.ws;

import java.time.Clock;
import java.util.Map;
import me.imshy.chat.auth.AccessTokenVerifier;
import me.imshy.chat.auth.AuthProperties;
import me.imshy.chat.auth.PublicSigningKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.server.WebFilter;

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
    HandlerMapping chatWebSocketMapping() {
        var mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of(WS_PATH, new ChatWebSocketHandler()));
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;
    }

    @Bean
    WebSocketHandlerAdapter chatWebSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
