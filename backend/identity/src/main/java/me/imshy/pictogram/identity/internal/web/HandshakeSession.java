package me.imshy.pictogram.identity.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tears down the servlet session Spring created to carry the authorization request across
 * the redirect to Google and back. {@code AbstractAuthenticationProcessingFilter} has
 * already persisted the OIDC {@code SecurityContext} into that session by the time
 * {@link OidcSignInSuccessHandler} runs, so every terminal outcome of the handshake —
 * success, an unusable account, an unexpected failure — must end it, or a partially
 * authenticated {@code JSESSIONID} lingers until it times out (#27).
 */
final class HandshakeSession {

    private HandshakeSession() {
    }

    static void end(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
