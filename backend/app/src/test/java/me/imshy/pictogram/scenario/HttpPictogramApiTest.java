package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import me.imshy.pictogram.scenario.PictogramApi.Actor;
import me.imshy.pictogram.scenario.PictogramApi.DeleteOutcome;
import me.imshy.pictogram.scenario.PictogramApi.Profile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Tag("fast")
class HttpPictogramApiTest {

    private HttpServer server;
    private URI baseUri;
    private final List<Recorded> received = new CopyOnWriteArrayList<>();
    private final Map<String, Stub> stubs = new ConcurrentHashMap<>();
    private final RecordingSignIn signIn = new RecordingSignIn();
    private HttpPictogramApi api;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
        api = new HttpPictogramApi(baseUri, JsonMapper.builder().build(), signIn);

        stub("POST", "/api/auth/refresh", 200, "{\"accessToken\":\"stub-access-token\"}");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void registerViaGoogleDrivesSignInThenRedeemsTheRefreshCookie() {
        api.registerViaGoogle("ada@example.com");

        assertThat(signIn.capturedEmail).isEqualTo("ada@example.com");
        Recorded redeem = only("POST", "/api/auth/refresh");
        assertThat(redeem.header("Cookie")).isEqualTo("pictogram_refresh=stub-refresh-cookie");
    }

    @Test
    void completeOnboardingSendsProfileJsonAndMapsThe201BodyToAProfile() {
        stub(
                "POST",
                "/api/profiles",
                201,
                "{\"userId\":\"u-1\",\"username\":\"ada_lovelace\",\"displayName\":\"Ada Lovelace\",\"bio\":\"Countess\"}");

        Profile profile = actor().completeOnboarding("ada_lovelace", "Ada Lovelace", "Countess");

        Recorded request = only("POST", "/api/profiles");
        assertThat(request.header("Authorization")).isEqualTo("Bearer stub-access-token");
        assertThat(request.header("Content-Type")).isEqualTo("application/json");
        JsonNode sent = request.bodyAsJson();
        assertThat(sent.size()).isEqualTo(3);
        assertThat(sent.path("username").asString()).isEqualTo("ada_lovelace");
        assertThat(sent.path("displayName").asString()).isEqualTo("Ada Lovelace");
        assertThat(sent.path("bio").asString()).isEqualTo("Countess");
        assertThat(profile).isEqualTo(new Profile("u-1", "ada_lovelace", "Ada Lovelace", "Countess"));
    }

    @Test
    void deletePostMaps204ToDeleted() {
        stub("DELETE", "/api/posts/p-1", 204, "");

        assertThat(actor().deletePost("p-1")).isEqualTo(DeleteOutcome.DELETED);
        assertThat(only("DELETE", "/api/posts/p-1").header("Authorization")).isEqualTo("Bearer stub-access-token");
    }

    @Test
    void deletePostMaps403ToForbidden() {
        stub("DELETE", "/api/posts/p-2", 403, "{\"detail\":\"not the author\"}");

        assertThat(actor().deletePost("p-2")).isEqualTo(DeleteOutcome.FORBIDDEN);
    }

    @Test
    void viewProfileMaps404ToEmpty() {
        stub("GET", "/api/profiles/ghost", 404, "");

        assertThat(actor().viewProfile("ghost")).isEmpty();
    }

    @Test
    void uploadPhotoSendsAWellFormedMultipartBodyAndReadsTheMediaId() {
        stub("POST", "/api/media", 201, "{\"mediaId\":\"m-42\"}");
        byte[] image = {1, 2, 3, 4, 5, 6, 7, 8};

        String mediaId = actor().uploadPhoto(image);

        Recorded request = only("POST", "/api/media");
        String contentType = request.header("Content-Type");
        assertThat(contentType).startsWith("multipart/form-data; boundary=");
        String boundary = contentType.substring(contentType.indexOf("boundary=") + "boundary=".length());
        String body = new String(request.body, StandardCharsets.ISO_8859_1);
        assertThat(body)
                .startsWith("--" + boundary + "\r\n")
                .contains("Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n")
                .contains("Content-Type: image/jpeg\r\n\r\n")
                .contains(new String(image, StandardCharsets.ISO_8859_1))
                .endsWith("\r\n--" + boundary + "--\r\n");
        assertThat(mediaId).isEqualTo("m-42");
    }

    private Actor actor() {
        return api.registerViaGoogle("ada@example.com");
    }

    private void stub(String method, String path, int status, String body) {
        stubs.put(method + " " + path, new Stub(status, body));
    }

    private Recorded only(String method, String path) {
        List<Recorded> matches = received.stream()
                .filter(r -> r.method.equals(method) && r.path.equals(path))
                .toList();
        assertThat(matches).as("requests to %s %s", method, path).hasSize(1);
        return matches.getFirst();
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        var recorded = new Recorded(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getRawQuery(),
                exchange.getRequestHeaders(),
                body);
        received.add(recorded);

        Stub stub = stubs.get(recorded.method + " " + recorded.path);
        if (stub == null) {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
            return;
        }
        byte[] out = stub.body.getBytes(StandardCharsets.UTF_8);
        if (out.length == 0) {
            exchange.sendResponseHeaders(stub.status, -1);
        } else {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(stub.status, out.length);
            exchange.getResponseBody().write(out);
        }
        exchange.close();
    }

    private record Stub(int status, String body) {}

    private static final class RecordingSignIn implements SignIn {

        private volatile String capturedEmail;

        @Override
        public String authenticate(String email) {
            this.capturedEmail = email;
            return "stub-refresh-cookie";
        }
    }

    private static final class Recorded {

        private final String method;
        private final String path;
        private final String query;
        private final Map<String, List<String>> headers;
        private final byte[] body;

        private Recorded(String method, String path, String query, Map<String, List<String>> headers, byte[] body) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.headers = headers;
            this.body = body;
        }

        private String header(String name) {
            return headers.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(name))
                    .flatMap(e -> e.getValue().stream())
                    .findFirst()
                    .orElse(null);
        }

        private JsonNode bodyAsJson() {
            return JsonMapper.builder().build().readTree(body);
        }
    }
}
