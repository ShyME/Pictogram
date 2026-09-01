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

class SignInCompletion {

    private final AuthProperties properties;

    SignInCompletion(AuthProperties properties) {
        this.properties = properties;
    }

    void succeeded(HttpServletRequest request, HttpServletResponse response, Session session) throws IOException {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                RefreshCookie.issue(session.refreshToken(), properties.refreshTokenTtl(), properties.cookieSecure())
                        .toString());
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
