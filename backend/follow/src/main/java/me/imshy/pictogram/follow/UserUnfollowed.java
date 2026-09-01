package me.imshy.pictogram.follow;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;

/**
 * A user stopped following another user — an edge removed from the follow graph. Emitted
 * only on a real state change: unfollowing someone not followed is a silent no-op. A
 * fan-out-on-write feed would drop the followed user's posts from the follower's timeline.
 * In v1 nothing consumes this; it is follow's forward contract (ADR-0002, CONTEXT-MAP).
 */
public record UserUnfollowed(UserId follower, UserId followed, Instant unfollowedAt) {
}
