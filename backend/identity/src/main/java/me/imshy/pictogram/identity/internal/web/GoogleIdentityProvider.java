package me.imshy.pictogram.identity.internal.web;

import me.imshy.pictogram.identity.internal.IdentityProvider;
import me.imshy.pictogram.identity.internal.web.UnusableGoogleAccountException.Reason;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

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
            throw new UnusableGoogleAccountException(Reason.EMAIL_MISSING,
                    "Google returned no email for subject " + user.getSubject());
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new UnusableGoogleAccountException(Reason.EMAIL_UNVERIFIED,
                    "Google has not verified the email for subject " + user.getSubject());
        }
        return new VerifiedGoogleAccount(user.getSubject(), email);
    }

    record VerifiedGoogleAccount(String subject, String email) {
    }
}
