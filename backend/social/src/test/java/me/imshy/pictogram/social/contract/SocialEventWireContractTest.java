package me.imshy.pictogram.social.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.social.SocialEvent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The cross-broker wire contract for {@code pictogram.social} (ADR-0015),
 * extending the cross-module consumer-contract pattern (#157). Modulith's Kafka
 * externaliser serialises the mapped payload with a plain Jackson
 * {@link JsonMapper} (its {@code ByteArrayJacksonJsonMessageConverter}
 * default), so serialising {@link SocialEvent} the same way here pins the exact
 * JSON — a renamed, dropped, or added field breaks this before it reaches a
 * consumer. {@code SocialEventKafkaRelayTest} spot-checks it end to end through
 * the real relay.
 */
class SocialEventWireContractTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private static final UserId RECIPIENT = new UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserId ACTOR = new UserId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final PostId SUBJECT = new PostId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final Instant WHEN = Instant.parse("2026-09-09T12:00:00Z");

    @Test
    void aLikeSerialisesToTheAgreedFieldsAndNothingElse() {
        assertThat(fields(SocialEvent.postLiked(RECIPIENT, ACTOR, SUBJECT, WHEN)))
            .containsExactlyInAnyOrderEntriesOf(Map.of("type", "post-liked", "recipientId", RECIPIENT.toString(),
                "actorId", ACTOR.toString(), "subjectId", SUBJECT.toString(), "occurredAt", "2026-09-09T12:00:00Z"));
    }

    @Test
    void aCommentSerialisesToTheAgreedFieldsAndNothingElse() {
        assertThat(fields(SocialEvent.postCommented(RECIPIENT, ACTOR, SUBJECT, WHEN)))
            .containsExactlyInAnyOrderEntriesOf(Map.of("type", "post-commented", "recipientId", RECIPIENT.toString(),
                "actorId", ACTOR.toString(), "subjectId", SUBJECT.toString(), "occurredAt", "2026-09-09T12:00:00Z"));
    }

    @Test
    void aFollowSerialisesToTheAgreedFieldsAndCarriesNoSubject() {
        assertThat(fields(SocialEvent.userFollowed(RECIPIENT, ACTOR, WHEN)))
            .containsExactlyInAnyOrderEntriesOf(Map.of("type", "user-followed", "recipientId", RECIPIENT.toString(),
                "actorId", ACTOR.toString(), "occurredAt", "2026-09-09T12:00:00Z"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fields(SocialEvent event) {
        return json.readValue(json.writeValueAsString(event), Map.class);
    }
}
