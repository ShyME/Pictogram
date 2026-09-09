package me.imshy.pictogram.social;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

/**
 * The wire shape of the three {@code social} engagement events once they leave
 * the JVM for the {@code pictogram.social} Kafka topic (ADR-0015): a
 * {@code type} discriminator plus the four things a notification needs — who it
 * is for, who caused it, what it is about, and when.
 * {@link me.imshy.pictogram.social.internal.SocialEventExternalization} maps
 * the in-process events onto this; {@code SocialEventWireContractTest} pins the
 * JSON.
 *
 * <p>
 * {@code recipientId} is the notification's target — the post's author for a
 * like or a comment, the followed user for a follow — resolved by
 * {@code social} at publish time so the consumer never calls back into
 * {@code post}. It is also the Kafka partition key. {@code subjectId} is the
 * liked / commented post, and is absent for a follow.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SocialEvent(Type type, UserId recipientId, UserId actorId, PostId subjectId, Instant occurredAt) {

    public enum Type {
        POST_LIKED, POST_COMMENTED, USER_FOLLOWED;

        @JsonValue
        String wireName() {
            return name().toLowerCase().replace('_', '-');
        }
    }

    public static SocialEvent postLiked(UserId recipientId, UserId actorId, PostId post, Instant occurredAt) {
        return new SocialEvent(Type.POST_LIKED, recipientId, actorId, post, occurredAt);
    }

    public static SocialEvent postCommented(UserId recipientId, UserId actorId, PostId post, Instant occurredAt) {
        return new SocialEvent(Type.POST_COMMENTED, recipientId, actorId, post, occurredAt);
    }

    public static SocialEvent userFollowed(UserId recipientId, UserId actorId, Instant occurredAt) {
        return new SocialEvent(Type.USER_FOLLOWED, recipientId, actorId, null, occurredAt);
    }
}
