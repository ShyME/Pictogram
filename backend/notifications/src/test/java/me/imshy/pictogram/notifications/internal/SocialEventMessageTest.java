package me.imshy.pictogram.notifications.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The consumer half of the {@code pictogram.social} wire contract (ADR-0015) —
 * the mirror of {@code social}'s {@code SocialEventWireContractTest}. The
 * consumer reads the topic as raw JSON and binds it to its own
 * {@link SocialEventMessage}; a renamed or dropped field breaks this.
 */
class SocialEventMessageTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private static final UserId RECIPIENT = UserId.fromString("11111111-1111-1111-1111-111111111111");
    private static final UserId ACTOR = UserId.fromString("22222222-2222-2222-2222-222222222222");
    private static final PostId SUBJECT = PostId.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void bindsALikeFromTheAgreedFields() {
        var message = read("""
            {"type":"post-liked","recipientId":"11111111-1111-1111-1111-111111111111",\
            "actorId":"22222222-2222-2222-2222-222222222222",\
            "subjectId":"33333333-3333-3333-3333-333333333333","occurredAt":"2026-09-09T12:00:00Z"}""");

        assertThat(message).isEqualTo(
            new SocialEventMessage("post-liked", RECIPIENT, ACTOR, SUBJECT, Instant.parse("2026-09-09T12:00:00Z")));
    }

    @Test
    void bindsAFollowWhichCarriesNoSubject() {
        var message = read("""
            {"type":"user-followed","recipientId":"11111111-1111-1111-1111-111111111111",\
            "actorId":"22222222-2222-2222-2222-222222222222","occurredAt":"2026-09-09T12:00:00Z"}""");

        assertThat(message.subjectId()).isNull();
        assertThat(message.type()).isEqualTo("user-followed");
    }

    @Test
    void aSelfActionIsOneWhoseActorIsItsRecipient() {
        assertThat(new SocialEventMessage("post-liked", RECIPIENT, RECIPIENT, SUBJECT, Instant.now()).isSelfAction())
            .isTrue();
        assertThat(new SocialEventMessage("post-liked", RECIPIENT, ACTOR, SUBJECT, Instant.now()).isSelfAction())
            .isFalse();
    }

    private SocialEventMessage read(String wire) {
        return json.readValue(wire, SocialEventMessage.class);
    }
}
