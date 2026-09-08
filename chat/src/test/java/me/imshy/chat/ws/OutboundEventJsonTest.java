package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import me.imshy.chat.UserId;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

// The exact bytes the frontend's parseInboundFrame (frontend/src/features/chat) reads off
// the wire. The "type" discriminant on each OutboundEvent is what the frontend switches on,
// so it is pinned here rather than left to drift with the record shape (#174).
class OutboundEventJsonTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UserId USER = UserId.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void aDeliveredMessageIsTaggedMessage() {
        assertThat(JSON.writeValueAsString(DeliveredMessage.of(USER, "hi there"))).isEqualTo(
            "{\"type\":\"message\",\"senderUserId\":\"11111111-1111-1111-1111-111111111111\",\"text\":\"hi there\"}");
    }

    @Test
    void anUndeliveredMessageIsTaggedUndelivered() {
        assertThat(JSON.writeValueAsString(UndeliveredMessage.of(USER, "hi there"))).isEqualTo(
            "{\"type\":\"undelivered\",\"recipientUserId\":\"11111111-1111-1111-1111-111111111111\",\"text\":\"hi there\"}");
    }

    @Test
    void aPresenceStatusIsTaggedPresence() {
        assertThat(JSON.writeValueAsString(PresenceStatus.of(USER, true)))
            .isEqualTo("{\"type\":\"presence\",\"userId\":\"11111111-1111-1111-1111-111111111111\",\"online\":true}");
    }
}
