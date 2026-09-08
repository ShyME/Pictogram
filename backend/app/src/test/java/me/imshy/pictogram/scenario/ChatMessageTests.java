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

// End to end across both services (#165): a message sent over one signed-in session's chat
// connection fans out to every open connection its recipient has, or reports undelivered
// back to the sender when the recipient has none — with no relationship check gating it.
interface ChatMessageTests extends AppUnderTest, ChatUnderTest {

    @Test
    default void aMessageIsDeliveredToEveryOpenConnectionTheRecipientHas() throws Exception {
        var alice = pictogram().registerViaGoogle("chat-msg-alice@example.com");
        var bob = pictogram().registerViaGoogle("chat-msg-bob@example.com");
        String aliceId = alice.completeOnboarding("ada_chatmsg").userId();
        String bobId = bob.completeOnboarding("bob_chatmsg").userId();

        RecordingListener bobTab1 = new RecordingListener();
        RecordingListener bobTab2 = new RecordingListener();
        connect(bob.accessToken(), bobTab1);
        connect(bob.accessToken(), bobTab2);
        WebSocket aliceSocket = connect(alice.accessToken(), new RecordingListener());

        send(aliceSocket, bobId, "hi bob");

        assertThat(bobTab1.await()).contains("\"type\":\"message\"").contains(aliceId).contains("hi bob");
        assertThat(bobTab2.await()).contains("\"type\":\"message\"").contains(aliceId).contains("hi bob");
    }

    @Test
    default void aMessageToAUserWithNoOpenConnectionIsReportedUndeliveredOnTheSendersConnection() throws Exception {
        var alice = pictogram().registerViaGoogle("chat-msg-carol@example.com");
        String recipientWithNoConnection = UUID.randomUUID().toString();

        RecordingListener senderListener = new RecordingListener();
        WebSocket aliceSocket = connect(alice.accessToken(), senderListener);

        send(aliceSocket, recipientWithNoConnection, "hello?");

        assertThat(senderListener.await()).contains("\"type\":\"undelivered\"").contains(recipientWithNoConnection)
            .contains("hello?");
    }

    @Test
    default void sendingIsUnrestrictedBetweenTwoUsersWithNoFollowRelationship() throws Exception {
        var alice = pictogram().registerViaGoogle("chat-msg-dave@example.com");
        var bob = pictogram().registerViaGoogle("chat-msg-erin@example.com");
        String aliceId = alice.completeOnboarding("ada_chatmsg2").userId();
        String bobId = bob.completeOnboarding("bob_chatmsg2").userId();

        RecordingListener bobListener = new RecordingListener();
        connect(bob.accessToken(), bobListener);
        WebSocket aliceSocket = connect(alice.accessToken(), new RecordingListener());

        send(aliceSocket, bobId, "hi, stranger");

        assertThat(bobListener.await()).contains("\"type\":\"message\"").contains(aliceId).contains("hi, stranger");
    }

    private static void send(WebSocket socket, String recipientUserId, String text) {
        socket.sendText("{\"recipientUserId\":\"%s\",\"text\":\"%s\"}".formatted(recipientUserId, text), true).join();
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

    // Collects the single frame the test cares about per connection: `onOpen` and
    // `onText`
    // must re-request explicitly since overriding either turns off java.net.http's
    // default auto-request-more-messages behaviour.
    class RecordingListener implements WebSocket.Listener {

        private final CompletableFuture<String> received = new CompletableFuture<>();
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                received.complete(buffer.toString());
            }
            webSocket.request(1);
            return null;
        }

        String await() throws Exception {
            return received.get(5, TimeUnit.SECONDS);
        }
    }
}
