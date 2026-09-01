package me.imshy.pictogram.identity.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import me.imshy.pictogram.identity.internal.ExternalAccount;
import me.imshy.pictogram.identity.internal.IdentityAuthentication;
import me.imshy.pictogram.identity.internal.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Runs once Spring Security has completed the Google OIDC handshake: mints the Pictogram
 * session and hands the outcome to {@link SignInCompletion}, which drops the refresh token
 * in its cookie and sends the browser on to the SPA (ADR-0004).
 *
 * <p>A {@link UnusableGoogleAccountException} out of {@link GoogleIdentityProvider#verify}
 * means Google authenticated the person but the account is unusable: no Pictogram session
 * is issued and the browser goes to the SPA's sign-in-error route instead.
 */
class OidcSignInSuccessHandler implements AuthenticationSuccessHandler {

    private final IdentityAuthentication authentication;
    private final GoogleIdentityProvider google;
    private final SignInCompletion completion;

    OidcSignInSuccessHandler(IdentityAuthentication authentication, GoogleIdentityProvider google,
            SignInCompletion completion) {
        this.authentication = authentication;
        this.google = google;
        this.completion = completion;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authenticationResult) throws IOException {
        var oidcUser = (OidcUser) authenticationResult.getPrincipal();
        GoogleIdentityProvider.VerifiedGoogleAccount verified;
        try {
            verified = google.verify(oidcUser);
        } catch (UnusableGoogleAccountException unusable) {
            completion.unusable(request, response, unusable.reason());
            return;
        }

        Session session = authentication.authenticate(
                new ExternalAccount(google.id(), verified.subject(), verified.email()));
        completion.succeeded(request, response, session);
    }
}
