package me.imshy.pictogram.identity.internal;

import me.imshy.pictogram.identity.internal.accesstoken.SigningKey;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
class AccessTokenSigningKeyConfiguration {

    private static final Log log = LogFactory.getLog(AccessTokenSigningKeyConfiguration.class);

    @Bean
    SigningKey accessTokenSigningKey(AuthProperties properties, Environment environment) {
        if (StringUtils.hasText(properties.signingKey())) {
            return SigningKey.fromJwkJson(properties.signingKey());
        }
        if (environment.matchesProfiles("prod")) {
            throw new IllegalStateException(
                "pictogram.auth.signing-key (env PICTOGRAM_AUTH_SIGNING_KEY) must be set on "
                    + "the 'prod' profile: an ephemeral key breaks token verification across replicas and invalidates "
                    + "every access token on restart (ADR-0004).");
        }
        log.warn("pictogram.auth.signing-key is not set — generating a process-lifetime access-token "
            + "key. Access tokens will not survive a restart; set the property in production.");
        return SigningKey.generate();
    }
}
