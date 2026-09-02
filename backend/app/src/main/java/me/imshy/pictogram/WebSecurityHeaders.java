package me.imshy.pictogram;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

final class WebSecurityHeaders {

    // Self-contained SPA: the Vite build emits only external, same-origin scripts and styles
    // (frontend/dist/index.html carries no inline <script>) and Tailwind v4 compiles to a
    // static stylesheet, so script-src and style-src both stay strict — no 'unsafe-inline'.
    // The one runtime-computed style, the crop image's position in SquareCropper, is written
    // property-by-property on the element's style object, which style-src does not govern.
    // img-src allows blob:/data: for the client-side crop preview (post/NewPostPage).
    private static final String CONTENT_SECURITY_POLICY = String.join(
            "; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' blob: data:",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "frame-ancestors 'none'",
            "form-action 'self'");

    private static final String PERMISSIONS_POLICY =
            "accelerometer=(), autoplay=(), camera=(), display-capture=(), encrypted-media=(), "
                    + "fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), "
                    + "midi=(), payment=(), usb=()";

    private WebSecurityHeaders() {}

    static void apply(HeadersConfigurer<HttpSecurity> headers) {
        headers.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY));
    }
}
