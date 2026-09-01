package me.imshy.pictogram.identity.internal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import me.imshy.pictogram.identity.internal.AuthProperties;
import me.imshy.pictogram.identity.internal.ExternalAccount;
import me.imshy.pictogram.identity.internal.IdentityAuthentication;
import me.imshy.pictogram.identity.internal.Session;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Runs once Spring Security has completed the Google OIDC handshake: mints the Pictogram
 * session, drops the refresh token in its cookie, and sends the browser on to the SPA,
 * which fetches its first access token from {@code POST /api/auth/refresh} (ADR-0004).
 *
 * <p>The handshake needed an HTTP session to hold the authorization request across the
 * redirect to Google and back. Once the Pictogram (stateless) session exists that servlet
 * session has no further purpose, so it is invalidated here — otherwise a {@code JSESSIONID}
 * would linger authenticated until it timed out, and {@code AuthController.logout} (on the
 * stateless chain) never touches it (#27).
 *
 * <p>A {@link UnusableGoogleAccountException} out of {@link GoogleIdentityProvider#verify}
 * is caught here: Google authenticated the person but the account is unusable, so the
 * browser goes to the SPA's sign-in-error route and no Pictogram session is issued.
 */
class OidcSignInSuccessHandler implements AuthenticationSuccessHandler {

    private final IdentityAuthentication authentication;
    private final GoogleIdentityProvider google;
    private final AuthProperties properties;

    OidcSignInSuccessHandler(IdentityAuthentication authentication, GoogleIdentityProvider google,
            AuthProperties properties) {
        this.authentication = authentication;
        this.google = google;
        this.properties = properties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authenticationResult) throws IOException {
        var oidcUser = (OidcUser) authenticationResult.getPrincipal();
        GoogleIdentityProvider.VerifiedGoogleAccount verified;
        try {
            verified = google.verify(oidcUser);
        } catch (UnusableGoogleAccountException unusable) {
            HandshakeSession.end(request);
            response.sendRedirect(SignInErrorRedirect.withReason(
                    properties.signInErrorRedirect(), unusable.reason().slug()));
            return;
        }

        Session session = authentication.authenticate(
                new ExternalAccount(google.id(), verified.subject(), verified.email()));

        response.addHeader(HttpHeaders.SET_COOKIE, RefreshCookie.issue(
                session.refreshToken(), properties.refreshTokenTtl(), properties.cookieSecure()).toString());
        HandshakeSession.end(request);
        response.sendRedirect(properties.postLoginRedirect());
    }
}
