package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import me.imshy.chat.UserId;
import me.imshy.chat.auth.AccessTokenVerifier;
import me.imshy.chat.auth.PublicSigningKey;
import me.imshy.chat.auth.TestAccessTokens;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class ChatHandshakeFilterTest {

    private static final String PATH = "/ws";

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC);
    private final TestAccessTokens tokens = new TestAccessTokens();
    private final ChatHandshakeFilter filter = new ChatHandshakeFilter(PATH, new AccessTokenVerifier(
        PublicSigningKey.fromJwkJson(tokens.publicJwkJson()).jwkSource(), TestAccessTokens.ISSUER, clock));

    @Test
    void aValidTokenIsAttributedToItsCallerAndTheRequestProceeds() {
        UserId sender = UserId.random();
        var exchange = exchangeWithProtocol(tokens.issue(sender, clock));
        var chainRan = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainRan).isTrue();
        assertThat(exchange.getAttributes()).containsEntry(ChatHandshakeFilter.USER_ID_ATTRIBUTE, sender);
    }

    @Test
    void aTamperedTokenIsRejectedBeforeTheChainRuns() {
        var exchange = exchangeWithProtocol(tokens.issueSignedByAnotherKey(UserId.random(), clock));
        var chainRan = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainRan).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anExpiredTokenIsRejectedBeforeTheChainRuns() {
        var exchange = exchangeWithProtocol(tokens.issueExpired(UserId.random(), clock));
        var chainRan = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainRan).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aMissingTokenIsRejectedBeforeTheChainRuns() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(PATH).build());
        var chainRan = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainRan).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aValidTokenAmongSeveralCommaJoinedOfferedProtocolsIsAccepted() {
        UserId sender = UserId.random();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(PATH).header("Sec-WebSocket-Protocol",
            "some-other-protocol, " + tokens.issue(sender, clock)));
        var chainRan = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainRan).isTrue();
        assertThat(exchange.getAttributes()).containsEntry(ChatHandshakeFilter.USER_ID_ATTRIBUTE, sender);
    }

    @Test
    void requestsOnOtherPathsAreNotIntercepted() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());
        var chainRan = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainRan.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainRan).isTrue();
    }

    private static MockServerWebExchange exchangeWithProtocol(String token) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(PATH).header("Sec-WebSocket-Protocol", token));
    }
}
