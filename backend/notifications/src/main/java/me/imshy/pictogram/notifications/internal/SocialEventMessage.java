package me.imshy.pictogram.notifications.internal;

import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

/**
 * The inbound shape of one {@code pictogram.social} record (ADR-0015): a
 * {@code type} discriminator plus who the notification is for, who caused it,
 * which post it is about ({@code null} for a follow), and when.
 *
 * <p>
 * The consumer owns this type rather than importing {@code social}'s
 * {@code SocialEvent} — the topic is the contract, not a shared class, exactly
 * as a real service boundary would have it. {@code SocialEventMessageTest} pins
 * the JSON this must accept.
 */
record SocialEventMessage(String type, UserId recipientId, UserId actorId, PostId subjectId, Instant occurredAt) {

    boolean isSelfAction() {
        return actorId.equals(recipientId);
    }
}
