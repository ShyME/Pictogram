package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

// End to end across both services (#202): a signed-in session batch-queries chat for the
// presence of several users at once and gets one answer per user, each matching that
// user's live connection state.
interface ChatPresenceTests extends AppUnderTest, ChatUnderTest {

    @Test
    default void aBatchPresenceQueryReportsEachRequestedUsersLiveConnectionState() throws Exception {
        var asker = pictogram().registerViaGoogle("chat-pres-asker@example.com");
        var connectedUser = pictogram().registerViaGoogle("chat-pres-online@example.com");
        String connectedId = connectedUser.completeOnboarding("ada_chatpres").userId();
        String userWithNoConnection = UUID.randomUUID().toString();

        connect(connectedUser.accessToken(), new CollectingListener(0));
        CollectingListener answers = new CollectingListener(2);
        WebSocket askerSocket = connect(asker.accessToken(), answers);

        askerSocket.sendText(
            "{\"type\":\"presence-query\",\"userIds\":[\"%s\",\"%s\"]}".formatted(connectedId, userWithNoConnection),
            true).join();

        String frames = answers.await();
        assertThat(frames).contains("\"type\":\"presence\"").contains(connectedId).contains(userWithNoConnection);
        assertThat(frames).contains("\"userId\":\"" + connectedId + "\",\"online\":true");
        assertThat(frames).contains("\"userId\":\"" + userWithNoConnection + "\",\"online\":false");
    }

    private WebSocket connect(String accessToken, WebSocket.Listener listener)
        throws InterruptedException, ExecutionException, TimeoutException {
        return HttpClient.newHttpClient().newWebSocketBuilder().subprotocols(CHAT_SUBPROTOCOL, accessToken)
            .buildAsync(chatWsUri(), listener).get(10, TimeUnit.SECONDS);
    }

    private URI chatWsUri() {
        URI base = chatBaseUri();
        String scheme = "https".equals(base.getScheme()) ? "wss" : "ws";
        return URI.create(scheme + "://" + base.getAuthority() + "/ws");
    }

    // Accumulates whole text frames until `expectedFrames` of them have arrived,
    // then
    // completes with everything received. `onText` must re-request explicitly —
    // overriding
    // it turns off java.net.http's default auto-request behaviour.
    class CollectingListener implements WebSocket.Listener {

        private final int expectedFrames;
        private final CompletableFuture<String> received = new CompletableFuture<>();
        private final StringBuilder all = new StringBuilder();
        private final StringBuilder current = new StringBuilder();
        private int completedFrames;

        CollectingListener(int expectedFrames) {
            this.expectedFrames = expectedFrames;
            if (expectedFrames == 0)
                received.complete("");
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            current.append(data);
            if (last) {
                all.append(current).append('\n');
                current.setLength(0);
                if (++completedFrames >= expectedFrames)
                    received.complete(all.toString());
            }
            webSocket.request(1);
            return null;
        }

        String await() throws Exception {
            return received.get(5, TimeUnit.SECONDS);
        }
    }
}
