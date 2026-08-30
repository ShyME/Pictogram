package me.imshy.pictogram.identity.internal.web;

import me.imshy.pictogram.identity.internal.IdentityProvider;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Turns a Google OIDC user into the provider-agnostic account the identity service works
 * with. This is where the {@link IdentityProvider} seam pays off: a future
 * {@code PasswordIdentityProvider} would produce the same shape from a login form without
 * anything downstream changing.
 */
@Component
class GoogleIdentityProvider implements IdentityProvider {

    static final String ID = "google";

    @Override
    public String id() {
        return ID;
    }

    VerifiedGoogleAccount verify(OidcUser user) {
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Google returned no email for subject " + user.getSubject());
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            // The email rides along in UserRegistered; downstream contexts must be able to trust it.
            throw new IllegalStateException("Google has not verified the email for subject " + user.getSubject());
        }
        return new VerifiedGoogleAccount(user.getSubject(), email);
    }

    record VerifiedGoogleAccount(String subject, String email) {
    }
}
