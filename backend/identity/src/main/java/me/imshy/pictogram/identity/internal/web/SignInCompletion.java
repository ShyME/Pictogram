package me.imshy.pictogram.identity.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import me.imshy.pictogram.identity.internal.AuthProperties;
import me.imshy.pictogram.identity.internal.Session;
import me.imshy.pictogram.identity.internal.web.UnusableGoogleAccountException.Reason;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The single place the Google OIDC handshake's outcome is turned into an HTTP response.
 * Before this, "what happens when the handshake ends" was re-implemented across three Spring
 * hooks that had already drifted apart — the success handler, the chain error filter, and
 * the failure handler.
 *
 * <p>Every terminal path ends the servlet session Spring created to carry the authorization
 * request across the redirect to Google and back: {@code AbstractAuthenticationProcessingFilter}
 * has persisted the OIDC {@code SecurityContext} into it, the sign-in chain has no stateless
 * logout to fall back on, so a partially authenticated {@code JSESSIONID} would linger until
 * it timed out (#27). Only {@link #succeeded} writes the refresh cookie, and it is the only
 * writer of that cookie on this chain.
 */
class SignInCompletion {

    private final AuthProperties properties;

    SignInCompletion(AuthProperties properties) {
        this.properties = properties;
    }

    void succeeded(HttpServletRequest request, HttpServletResponse response, Session session) throws IOException {
        response.addHeader(HttpHeaders.SET_COOKIE, RefreshCookie.issue(
                session.refreshToken(), properties.refreshTokenTtl(), properties.cookieSecure()).toString());
        endHandshakeSession(request);
        response.sendRedirect(properties.postLoginRedirect());
    }

    void unusable(HttpServletRequest request, HttpServletResponse response, Reason reason) throws IOException {
        endHandshakeSession(request);
        response.sendRedirect(signInErrorRedirect(reason.slug()));
    }

    void failed(HttpServletRequest request, HttpServletResponse response) throws IOException {
        endHandshakeSession(request);
        response.sendRedirect(properties.signInErrorRedirect());
    }

    /**
     * Exposed for {@link OidcChainErrorFilter}'s backstop: when an unhandled throwable is
     * about to be rendered as problem+json rather than a redirect, the handshake session
     * still has to be torn down.
     */
    void endHandshakeSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private String signInErrorRedirect(String reasonSlug) {
        return UriComponentsBuilder.fromUriString(properties.signInErrorRedirect())
                .replaceQueryParam("error", reasonSlug)
                .build()
                .toUriString();
    }
}
