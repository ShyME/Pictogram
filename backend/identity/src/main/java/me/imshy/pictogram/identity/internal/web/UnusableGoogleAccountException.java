package me.imshy.pictogram.identity.internal.web;

/**
 * Google authenticated the person, but the account it handed back can't be used: the email
 * is unverified, or there is no email claim at all. The email rides along in
 * {@code UserRegistered} and downstream contexts must be able to trust it, so identity
 * refuses the sign-in here rather than registering a user on a shaky address.
 *
 * <p>Thrown from {@link GoogleIdentityProvider#verify} — inside {@code onAuthenticationSuccess},
 * after the OIDC handshake has already succeeded — so {@link OidcSignInSuccessHandler}
 * catches it and sends the browser to the SPA's sign-in-error route instead of letting it
 * render a white-label 500 (#27). The {@link Reason#slug()} becomes the {@code ?error=}
 * query parameter the SPA branches on.
 */
class UnusableGoogleAccountException extends RuntimeException {

    enum Reason {
        EMAIL_UNVERIFIED("email-unverified"),
        EMAIL_MISSING("email-missing");

        private final String slug;

        Reason(String slug) {
            this.slug = slug;
        }

        String slug() {
            return slug;
        }
    }

    private final Reason reason;

    UnusableGoogleAccountException(Reason reason, String detail) {
        super(detail);
        this.reason = reason;
    }

    Reason reason() {
        return reason;
    }
}
