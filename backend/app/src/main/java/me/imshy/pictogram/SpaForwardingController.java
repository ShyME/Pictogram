package me.imshy.pictogram;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the bundled SPA on its own client-side routes (#34). React Router owns paths like
 * {@code /login} and {@code /onboarding}; a hard reload, a pasted link, or a fresh tab on
 * one of them arrives at the backend with no matching handler. This forwards those requests
 * back to {@code index.html} (HTTP 200) so the SPA boots and re-resolves the route in the
 * browser.
 *
 * <p>The mapping is deliberately narrow: a single extension-less path segment outside
 * {@link #RESERVED_PREFIXES} (the API, actuator, OIDC and OpenAPI roots — everything that
 * has its own handler or filter chain), plus the one nested route the SPA has —
 * {@code /u/<username>}, the public profile page. Nothing else is touched: {@code /api/**}
 * stays a Problem Detail 404, {@code /login/oauth2/**} stays with the identity filter
 * chain, the static resource handler serves {@code /assets/*} and {@code /favicon.*}
 * (their file extensions keep them out of these patterns), and {@code GET /} keeps going
 * through Spring Boot's welcome-page handler. Any further nested SPA route needs its own
 * pattern added here.
 */
@Controller
class SpaForwardingController {

    /** Top-level path segments that own their own handler or filter chain, so never the SPA. */
    private static final String RESERVED_PREFIXES = "api|actuator|oauth2|v3";

    @GetMapping({
        "/{route:(?!(?:" + RESERVED_PREFIXES + ")$)[^.]+}",
        "/u/{username:[^.]+}"
    })
    String forwardToSpaShell() {
        return "forward:/index.html";
    }
}
