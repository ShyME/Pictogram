package me.imshy.pictogram.identity.internal;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.time.Clock;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Builds the {@link NimbusJwtDecoder} that verifies a Pictogram access token — ES256
 * signature against the public key, plus issuer and expiry. Spring's {@code NimbusJwtDecoder}
 * builders only cover RSA public keys and remote JWK sets, so the processor is assembled by
 * hand from the in-process EC key. The {@code :app} resource server and
 * {@link AccessTokens#resolve} share this one decoder.
 */
final class AccessTokenVerification {

    private AccessTokenVerification() {
    }

    static NimbusJwtDecoder decoder(JWKSource<SecurityContext> jwkSource, String issuer, Clock clock) {
        var processor = new DefaultJWTProcessor<SecurityContext>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, jwkSource));
        // Spring's validators below own claim checks; keep Nimbus from applying its own
        // system-clock expiry check so a test Clock fully controls token lifetime.
        processor.setJWTClaimsSetVerifier((claims, context) -> { });

        var timestamps = new JwtTimestampValidator();
        timestamps.setClock(clock);

        var decoder = new NimbusJwtDecoder(processor);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(issuer), timestamps));
        return decoder;
    }
}
