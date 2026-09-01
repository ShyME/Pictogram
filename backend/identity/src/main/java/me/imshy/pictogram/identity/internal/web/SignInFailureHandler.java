package me.imshy.pictogram.identity.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Takes over when the Google OIDC handshake itself fails — the user declined consent, the
 * state didn't match, the token exchange errored. Delegates to {@link SignInCompletion},
 * which lands the browser on the front-end sign-in-error route and — like every other
 * terminal outcome of the handshake — ends the handshake servlet session (#27).
 */
class SignInFailureHandler implements AuthenticationFailureHandler {

    private static final Log log = LogFactory.getLog(SignInFailureHandler.class);

    private final SignInCompletion completion;

    SignInFailureHandler(SignInCompletion completion) {
        this.completion = completion;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        log.info("Google sign-in did not complete: " + exception.getMessage());
        completion.failed(request, response);
    }
}
