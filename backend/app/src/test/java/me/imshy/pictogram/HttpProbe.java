package me.imshy.pictogram;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class HttpProbe {

    private final URI baseUri;

    HttpProbe(String baseUri) {
        this.baseUri = URI.create(baseUri);
    }

    HttpResponse<String> get(String path) {
        return exchange(to(path).header("Accept", "text/html").GET());
    }

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
