package me.imshy.pictogram.scenario;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;

final class HttpHelper {

    static final String REFRESH_COOKIE = "pictogram_refresh";

    private HttpHelper() {
    }

    static HttpResponse<String> send(HttpClient client, HttpRequest request) {
        try {
            return client.send(request, BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("HTTP call failed: " + request.method() + " " + request.uri(), e);
        }
    }

    static void require(HttpResponse<String> response, int expectedStatus, String action) {
        if (response.statusCode() != expectedStatus) {
            throw new AssertionError("Expected %d to %s but got %d: %s".formatted(expectedStatus, action,
                response.statusCode(), response.body()));
        }
    }

    static Optional<String> refreshCookie(CookieManager cookies) {
        return cookies.getCookieStore().getCookies().stream().filter(cookie -> REFRESH_COOKIE.equals(cookie.getName()))
            .map(HttpCookie::getValue).findFirst();
    }
}
