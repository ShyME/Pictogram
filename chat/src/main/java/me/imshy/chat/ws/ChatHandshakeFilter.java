package me.imshy.chat.ws;

import java.util.List;
import me.imshy.chat.UserId;
import me.imshy.chat.auth.AccessTokenVerifier;
import me.imshy.chat.auth.InvalidAccessTokenException;
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

        UserId caller = resolveCaller(exchange.getRequest().getHeaders().get("Sec-WebSocket-Protocol"));
        if (caller == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        exchange.getAttributes().put(USER_ID_ATTRIBUTE, caller);
        return chain.filter(exchange);
    }

    // RFC 6455 lets a client offer several subprotocols as one comma-separated
    // header
    // value (and a proxy may merge repeated header lines the same way) — try every
    // candidate rather than assuming the token is the only, or the first, one
    // offered.
    private UserId resolveCaller(List<String> offeredProtocols) {
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
