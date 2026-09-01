package me.imshy.pictogram.identity.internal.web;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds the front-end URL the browser is sent to when Google sign-in can't complete. The
 * configured {@code pictogram.auth.sign-in-error-redirect} carries the generic reason
 * ({@code /login?error=sign-in-failed}); a specific failure swaps the {@code error} query
 * parameter for its own slug so the SPA can show a tailored message (#27).
 */
final class SignInErrorRedirect {

    private SignInErrorRedirect() {
    }

    static String withReason(String configured, String reasonSlug) {
        return UriComponentsBuilder.fromUriString(configured)
                .replaceQueryParam("error", reasonSlug)
                .build()
                .toUriString();
    }
}
