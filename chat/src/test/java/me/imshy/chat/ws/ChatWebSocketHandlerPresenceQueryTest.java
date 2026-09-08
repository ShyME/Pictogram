package me.imshy.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// The batch presence frame (#202) shares one handler branch with the original single form.
// The "userIds" array is capped so an over-long request is bounded work rather than a walk
// of the whole list — mirrors the outbound-buffer treatment in ChatWebSocketHandlerBufferTest.
class ChatWebSocketHandlerPresenceQueryTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void theSingleUserIdFormYieldsThatOneSubject() {
        JsonNode frame = JSON.readTree("{\"type\":\"presence-query\",\"userId\":\"u-ada\"}");

        assertThat(ChatWebSocketHandler.presenceQuerySubjects(frame)).containsExactly("u-ada");
    }

    @Test
    void theBatchUserIdsFormYieldsEverySubjectInOrder() {
        JsonNode frame = JSON.readTree("{\"type\":\"presence-query\",\"userIds\":[\"u-ada\",\"u-bob\",\"u-cat\"]}");

        assertThat(ChatWebSocketHandler.presenceQuerySubjects(frame)).containsExactly("u-ada", "u-bob", "u-cat");
    }

    @Test
    void anOverLongUserIdsArrayIsTruncatedRatherThanWalkedInFull() {
        String ids = IntStream.range(0, ChatWebSocketHandler.MAX_PRESENCE_QUERY_SUBJECTS * 3)
            .mapToObj(i -> "\"u-" + i + "\"").collect(Collectors.joining(","));
        JsonNode frame = JSON.readTree("{\"type\":\"presence-query\",\"userIds\":[" + ids + "]}");

        assertThat(ChatWebSocketHandler.presenceQuerySubjects(frame))
            .hasSize(ChatWebSocketHandler.MAX_PRESENCE_QUERY_SUBJECTS);
    }

    // answerPresenceQuery emits one answer per subject synchronously into the
    // outbound
    // sink; a batch anywhere near OUTBOUND_BUFFER_CAPACITY would overflow it
    // mid-loop and
    // terminate the connection (ChatWebSocketHandlerBufferTest covers that path).
    // The cap
    // has to stay well below the buffer, with room to spare for concurrent message
    // traffic.
    @Test
    void theSubjectCapLeavesOutboundBufferHeadroom() {
        assertThat(ChatWebSocketHandler.MAX_PRESENCE_QUERY_SUBJECTS)
            .isLessThanOrEqualTo(ChatWebSocketHandler.OUTBOUND_BUFFER_CAPACITY / 2);
    }
}
