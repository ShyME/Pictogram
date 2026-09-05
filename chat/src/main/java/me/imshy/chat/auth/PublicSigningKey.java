package me.imshy.chat.auth;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.text.ParseException;

public final class PublicSigningKey {

    private final ECKey jwk;

    private PublicSigningKey(ECKey jwk) {
        this.jwk = jwk;
    }

    public static PublicSigningKey fromJwkJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(
                "pictogram.auth.public-key (env PICTOGRAM_AUTH_PUBLIC_KEY) must be set to identity's public "
                    + "signing JWK — chat has no key of its own to fall back to (ADR-0014)");
        }
        ECKey parsed;
        try {
            parsed = ECKey.parse(json);
        } catch (ParseException e) {
            throw new IllegalArgumentException("pictogram.auth.public-key is not a valid EC JWK", e);
        }
        if (parsed.isPrivate()) {
            // ADR-0014: chat verifies signatures but holds no signing material.
            throw new IllegalArgumentException("pictogram.auth.public-key must not include the private key");
        }
        return new PublicSigningKey(parsed);
    }

    public JWKSource<SecurityContext> jwkSource() {
        return new ImmutableJWKSet<>(new JWKSet(jwk));
    }
}
