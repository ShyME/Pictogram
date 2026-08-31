package me.imshy.pictogram;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * A minimal HTTP client for the {@code :app} end-to-end tests — the in-process
 * {@code @SpringBootTest} on a random port and the black-box suite against the running
 * container both drive the edge over real HTTP, differing only in the base URI. Bodies
 * come back as strings; 4xx/5xx are returned, not thrown.
 */
final class HttpProbe {

    private final URI baseUri;

    HttpProbe(String baseUri) {
        this.baseUri = URI.create(baseUri);
    }

    /** A browser-style {@code GET} — {@code Accept: text/html}, the deep-link case. */
    HttpResponse<String> get(String path) {
        return exchange(to(path).header("Accept", "text/html").GET());
    }

    /** {@code newBuilder} for {@code path} resolved against the base URI, for the odd request. */
    HttpRequest.Builder to(String path) {
        return HttpRequest.newBuilder(baseUri.resolve(path));
    }

    HttpResponse<String> exchange(HttpRequest.Builder request) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("HTTP request to " + baseUri + " failed", e);
        }
    }

    static String contentType(HttpResponse<?> response) {
        return response.headers().firstValue("Content-Type").orElse("");
    }
}
