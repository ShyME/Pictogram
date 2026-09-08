package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

// End to end across both services (#164): a signed-in session's real access token is
// accepted at chat's WebSocket handshake; a tampered one isn't.
interface ChatConnectionTests extends AppUnderTest, ChatUnderTest {

    @Test
    default void aSignedInSessionsAccessTokenIsAcceptedAtTheChatHandshake() throws Exception {
        var viewer = pictogram().registerViaGoogle("chat-handshake@example.com");

        WebSocket socket = connect(viewer.accessToken());

        assertThat(socket).isNotNull();
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
    }

    @Test
    default void aTamperedAccessTokenIsRejectedAtTheChatHandshake() {
        var viewer = pictogram().registerViaGoogle("chat-tampered@example.com");
        String tampered = tamper(viewer.accessToken());

        assertThatThrownBy(() -> connect(tampered)).isInstanceOfAny(ExecutionException.class, TimeoutException.class);
    }

    private WebSocket connect(String accessToken) throws InterruptedException, ExecutionException, TimeoutException {
        return HttpClient.newHttpClient().newWebSocketBuilder().subprotocols(CHAT_SUBPROTOCOL, accessToken)
            .buildAsync(chatWsUri(), new WebSocket.Listener() {
            }).get(10, TimeUnit.SECONDS);
    }

    private URI chatWsUri() {
        URI base = chatBaseUri();
        String scheme = "https".equals(base.getScheme()) ? "wss" : "ws";
        return URI.create(scheme + "://" + base.getAuthority() + "/ws");
    }

    private static String tamper(String jwt) {
        String[] parts = jwt.split("\\.");
        byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
        signature[0] ^= 0xFF;
        return parts[0] + "." + parts[1] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }
}
