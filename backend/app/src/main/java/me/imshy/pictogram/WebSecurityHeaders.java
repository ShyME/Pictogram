package me.imshy.pictogram;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

final class WebSecurityHeaders {

    private static final String CONTENT_SECURITY_POLICY = String.join("; ", "default-src 'self'", "script-src 'self'",
        "style-src 'self'", "img-src 'self' blob: data:", "font-src 'self'",
        // Temporary: chat answers on its own origin at a fixed port (mirrors
        // compose.yaml/Caddyfile's 8082 default and frontend/chatConnection.ts) until
        // #169 folds it behind this origin, at which point this goes back to bare
        // 'self'.
        "connect-src 'self' ws://*:8082 wss://*:8082", "object-src 'none'", "base-uri 'self'", "frame-ancestors 'none'",
        "form-action 'self'");

    private static final String PERMISSIONS_POLICY = "accelerometer=(), autoplay=(), camera=(), display-capture=(), encrypted-media=(), "
        + "fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), "
        + "midi=(), payment=(), usb=()";

    private WebSecurityHeaders() {
    }

    static void apply(HeadersConfigurer<HttpSecurity> headers) {
        headers.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            .permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY));
    }
}
