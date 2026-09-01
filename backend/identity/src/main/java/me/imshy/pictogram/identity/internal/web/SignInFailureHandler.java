package me.imshy.pictogram.identity.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import me.imshy.pictogram.identity.internal.AuthProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Takes over when the Google OIDC handshake itself fails — the user declined consent, the
 * state didn't match, the token exchange errored. Instead of Spring's default redirect to
 * {@code /login?error} (or a white-label page), the browser lands on the configured
 * front-end sign-in-error route (#27).
 */
class SignInFailureHandler implements AuthenticationFailureHandler {

    private static final Log log = LogFactory.getLog(SignInFailureHandler.class);

    private final AuthProperties properties;

    SignInFailureHandler(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        log.info("Google sign-in did not complete: " + exception.getMessage());
        response.sendRedirect(properties.signInErrorRedirect());
    }
}
