package me.imshy.chat.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.time.Clock;
import java.util.UUID;
import me.imshy.chat.UserId;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

public class AccessTokenVerifier {

    private final NimbusJwtDecoder decoder;

    public AccessTokenVerifier(JWKSource<SecurityContext> jwkSource, String issuer, Clock clock) {
        var processor = new DefaultJWTProcessor<SecurityContext>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, jwkSource));
        processor.setJWTClaimsSetVerifier((claims, context) -> {
        });

        var timestamps = new JwtTimestampValidator();
        timestamps.setClock(clock);

        decoder = new NimbusJwtDecoder(processor);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtIssuerValidator(issuer), timestamps));
    }

    public UserId resolve(String accessToken) {
        String subject;
        try {
            subject = decoder.decode(accessToken).getSubject();
        } catch (JwtException invalid) {
            throw new InvalidAccessTokenException("Not a valid Pictogram access token", invalid);
        }
        try {
            return new UserId(UUID.fromString(subject));
        } catch (IllegalArgumentException | NullPointerException notAUserId) {
            throw new InvalidAccessTokenException("Access token subject is not a Pictogram user id", notAUserId);
        }
    }
}
