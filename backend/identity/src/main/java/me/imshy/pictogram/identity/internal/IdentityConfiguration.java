package me.imshy.pictogram.identity.internal;

import java.time.Clock;
import me.imshy.pictogram.identity.PictogramAccessTokens;
import me.imshy.pictogram.identity.internal.accesstoken.AccessTokenVerification;
import me.imshy.pictogram.identity.internal.accesstoken.AccessTokens;
import me.imshy.pictogram.identity.internal.accesstoken.SigningKey;
import me.imshy.pictogram.identity.internal.refreshtoken.RefreshTokenService;
import me.imshy.pictogram.identity.internal.refreshtoken.RefreshTokens;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class IdentityConfiguration {

    @Bean
    @Primary
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
        return new RefreshTokenService(refreshTokens, clock, properties.refreshTokenTtl(),
            properties.refreshTokenRotationGrace(), txManager);
    }

    @Bean
    PictogramAccessTokens pictogramAccessTokens(AccessTokens accessTokens) {
        return accessTokens::resolve;
    }
}
