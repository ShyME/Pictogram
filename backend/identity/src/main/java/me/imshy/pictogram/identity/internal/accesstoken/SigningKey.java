package me.imshy.pictogram.identity.internal.accesstoken;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.text.ParseException;
import java.util.UUID;

/**
 * The elliptic-curve (P-256) key pair identity signs access tokens with. In production the
 * private JWK comes from {@code pictogram.auth.signing-key}; when that is blank a fresh key
 * is generated for the lifetime of the process (fine for local and test).
 */
public final class SigningKey {

    private final ECKey jwk;

    private SigningKey(ECKey jwk) {
        this.jwk = jwk;
    }

    public static SigningKey generate() {
        try {
            return new SigningKey(new ECKeyGenerator(Curve.P_256)
                    .keyID(UUID.randomUUID().toString())
                    .generate());
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not generate an access-token signing key", e);
        }
    }

    public static SigningKey fromJwkJson(String json) {
        try {
            ECKey parsed = ECKey.parse(json);
            if (!parsed.isPrivate()) {
                throw new IllegalArgumentException("pictogram.auth.signing-key must include the private key");
            }
            return new SigningKey(parsed);
        } catch (ParseException e) {
            throw new IllegalArgumentException("pictogram.auth.signing-key is not a valid EC JWK", e);
        }
    }

    public JWKSource<SecurityContext> jwkSource() {
        return new ImmutableJWKSet<>(new JWKSet(jwk));
    }
}
