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
import org.springframework.security.oauth2.jwt.Jwt;
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

    public ResolvedAccessToken resolve(String accessToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(accessToken);
        } catch (JwtException invalid) {
            throw new InvalidAccessTokenException("Not a valid Pictogram access token", invalid);
        }
        UserId userId;
        try {
            userId = new UserId(UUID.fromString(jwt.getSubject()));
        } catch (IllegalArgumentException | NullPointerException notAUserId) {
            throw new InvalidAccessTokenException("Access token subject is not a Pictogram user id", notAUserId);
        }
        // JwtTimestampValidator accepts a token with no "exp" claim at all; ChatWebSocketHandler
        // needs a real expiry to schedule its close (#192), so a missing one is rejected here
        // rather than reaching that Duration.between as a null.
        if (jwt.getExpiresAt() == null)
            throw new InvalidAccessTokenException("Access token has no expiry", null);
        return new ResolvedAccessToken(userId, jwt.getExpiresAt());
    }
}
