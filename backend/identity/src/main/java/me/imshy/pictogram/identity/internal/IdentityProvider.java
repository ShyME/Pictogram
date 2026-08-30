package me.imshy.pictogram.identity.internal;

/**
 * A way of proving who a person is. {@code GoogleIdentityProvider} is the only one in v1;
 * the seam exists so email/password can be added as a second provider without touching
 * {@link IdentityAuthentication} or the web edge — it would verify a credential and hand
 * back an {@link ExternalAccount} the same way (ADR-0004).
 */
public interface IdentityProvider {

    /** The value stored on {@code app_user.provider} for accounts this provider vouches for. */
    String id();
}
