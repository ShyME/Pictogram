package me.imshy.chat.ws;

import java.util.List;
import me.imshy.chat.auth.AccessTokenVerifier;
import me.imshy.chat.auth.InvalidAccessTokenException;
import me.imshy.chat.auth.ResolvedAccessToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Verifies the Pictogram access token carried as the WebSocket subprotocol
 * before the upgrade completes (ADR-0014) — a rejected token never reaches
 * {@code 101 Switching
 * Protocols}.
 */
public class ChatHandshakeFilter implements WebFilter {

    public static final String USER_ID_ATTRIBUTE = "chat.userId";

    // Carried alongside USER_ID_ATTRIBUTE so ChatWebSocketHandler can close the connection
    // when the token it was handshaked with expires (#192), rather than trusting a
    // once-at-handshake check for the socket's whole (much longer) lifetime.
    public static final String EXPIRES_AT_ATTRIBUTE = "chat.accessTokenExpiresAt";

    private final String path;
    private final AccessTokenVerifier verifier;

    public ChatHandshakeFilter(String path, AccessTokenVerifier verifier) {
        this.path = path;
        this.verifier = verifier;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!path.equals(exchange.getRequest().getPath().value()))
            return chain.filter(exchange);

        ResolvedAccessToken caller = resolveCaller(exchange.getRequest().getHeaders().get("Sec-WebSocket-Protocol"));
        if (caller == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        exchange.getAttributes().put(USER_ID_ATTRIBUTE, caller.userId());
        exchange.getAttributes().put(EXPIRES_AT_ATTRIBUTE, caller.expiresAt());
        return chain.filter(exchange);
    }

    // RFC 6455 lets a client offer several subprotocols as one comma-separated
    // header
    // value (and a proxy may merge repeated header lines the same way) — try every
    // candidate rather than assuming the token is the only, or the first, one
    // offered.
    private ResolvedAccessToken resolveCaller(List<String> offeredProtocols) {
        if (offeredProtocols == null)
            return null;
        for (String header : offeredProtocols) {
            for (String candidate : header.split(",")) {
                String token = candidate.trim();
                if (token.isEmpty())
                    continue;
                try {
                    return verifier.resolve(token);
                } catch (InvalidAccessTokenException invalid) {
                    // not this one — keep looking at the remaining candidates
                }
            }
        }
        return null;
    }
}
