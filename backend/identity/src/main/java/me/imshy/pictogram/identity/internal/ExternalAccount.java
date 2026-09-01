package me.imshy.pictogram.identity.internal;

import java.util.Objects;

public record ExternalAccount(String provider, String subject, String email) {

    public ExternalAccount {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(email, "email");
    }
}
