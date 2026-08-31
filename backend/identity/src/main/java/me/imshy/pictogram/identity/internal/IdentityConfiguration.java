package me.imshy.pictogram.identity.internal;

import java.time.Clock;
import me.imshy.pictogram.identity.PictogramAccessTokens;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

/**
 * Assembles identity's token machinery. The {@link JwtDecoder} published here is what the
 * {@code :app} resource server verifies Pictogram access tokens with (ADR-0004).
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class IdentityConfiguration {

    private static final Log log = LogFactory.getLog(IdentityConfiguration.class);

    @Bean
    SigningKey accessTokenSigningKey(AuthProperties properties) {
        if (StringUtils.hasText(properties.signingKey())) {
            return SigningKey.fromJwkJson(properties.signingKey());
        }
        log.warn("pictogram.auth.signing-key is not set — generating a process-lifetime access-token "
                + "key. Access tokens will not survive a restart; set the property in production.");
        return SigningKey.generate();
    }

    @Bean
    JwtDecoder jwtDecoder(SigningKey signingKey, AuthProperties properties, Clock clock) {
        return AccessTokenVerification.decoder(signingKey.jwkSource(), properties.issuer(), clock);
    }

    @Bean
    AccessTokens accessTokens(SigningKey signingKey, JwtDecoder jwtDecoder, AuthProperties properties, Clock clock) {
        return new AccessTokens(new NimbusJwtEncoder(signingKey.jwkSource()), jwtDecoder, clock,
                properties.accessTokenTtl(), properties.issuer());
    }

    @Bean
    RefreshTokenService refreshTokenService(RefreshTokens refreshTokens, AuthProperties properties, Clock clock,
            PlatformTransactionManager txManager) {
        return new RefreshTokenService(refreshTokens, clock, properties.refreshTokenTtl(), txManager);
    }

    @Bean
    PictogramAccessTokens pictogramAccessTokens(AccessTokens accessTokens) {
        return accessTokens::resolve;
    }
}
