package me.imshy.pictogram.identity.internal;

import java.util.Objects;

/**
 * A verified identity handed to {@link IdentityAuthentication} by an {@link IdentityProvider}.
 * Google OIDC is the only provider in v1; a future email/password provider produces the same
 * shape, which is what keeps {@code authenticate} provider-agnostic (ADR-0004).
 */
public record ExternalAccount(String provider, String subject, String email) {

    public ExternalAccount {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(email, "email");
    }
}
