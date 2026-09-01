package me.imshy.pictogram.identity.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import me.imshy.pictogram.identity.internal.web.UnusableGoogleAccountException.Reason;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class GoogleIdentityProviderTest {

    private final GoogleIdentityProvider google = new GoogleIdentityProvider();

    @Test
    void aVerifiedEmailYieldsTheAccount() {
        OidcUser user = oidcUser("google-sub-1", "ada@example.com", true);

        var account = google.verify(user);

        assertThat(account.subject()).isEqualTo("google-sub-1");
        assertThat(account.email()).isEqualTo("ada@example.com");
    }

    @Test
    void anUnverifiedEmailIsRejected() {
        OidcUser user = oidcUser("google-sub-2", "grace@example.com", false);

        assertThatExceptionOfType(UnusableGoogleAccountException.class)
                .isThrownBy(() -> google.verify(user))
                .satisfies(ex -> assertThat(ex.reason()).isEqualTo(Reason.EMAIL_UNVERIFIED));
    }

    @Test
    void aMissingEmailClaimIsRejected() {
        OidcUser user = oidcUser("google-sub-3", null, true);

        assertThatExceptionOfType(UnusableGoogleAccountException.class)
                .isThrownBy(() -> google.verify(user))
                .satisfies(ex -> assertThat(ex.reason()).isEqualTo(Reason.EMAIL_MISSING));
    }

    private static OidcUser oidcUser(String subject, String email, boolean emailVerified) {
        var idToken = OidcIdToken.withTokenValue("token")
                .subject(subject)
                .claim("email_verified", emailVerified);
        if (email != null) {
            idToken.claim("email", email);
        }
        return new DefaultOidcUser(List.of(), idToken.build());
    }
}
