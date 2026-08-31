package me.imshy.pictogram.profile;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;

/**
 * A user edited their profile — display name, bio, or a username rename. {@code username} is
 * the handle as it stands <em>after</em> the edit, so a consumer that caches author handles
 * (a fan-out-on-write feed, notifications) can refresh them. In v1 nothing consumes this; it
 * is profile's forward contract (ADR-0002, CONTEXT-MAP).
 */
public record ProfileUpdated(UserId userId, String username, Instant updatedAt) {
}
