package me.imshy.pictogram.identity.internal.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;

final class RefreshCookie {

    static final String NAME = "pictogram_refresh";
    private static final String PATH = "/api/auth";

    private RefreshCookie() {
    }

    static ResponseCookie issue(String token, Duration maxAge, boolean secure) {
        return builder(token, secure).maxAge(maxAge).build();
    }

    static ResponseCookie expired(boolean secure) {
        return builder("", secure).maxAge(0).build();
    }

    static Optional<String> readFrom(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private static ResponseCookie.ResponseCookieBuilder builder(String value, boolean secure) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH);
    }
}
