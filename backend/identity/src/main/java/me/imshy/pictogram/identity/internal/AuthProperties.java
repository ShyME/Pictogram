package me.imshy.pictogram.identity.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("pictogram.auth")
public record AuthProperties(
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("30d") Duration refreshTokenTtl,
        @DefaultValue("60s") Duration refreshTokenRotationGrace,
        @DefaultValue("pictogram") String issuer,
        String signingKey,
        @DefaultValue("/") String postLoginRedirect,
        @DefaultValue("/login?error=sign-in-failed") String signInErrorRedirect,
        @DefaultValue("true") boolean cookieSecure) {}
