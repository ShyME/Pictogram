package me.imshy.pictogram.follow;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;

/**
 * A user started following another user — a new edge in the follow graph. Emitted only on a
 * real state change: following someone already followed is a silent no-op. A fan-out-on-write
 * feed would backfill the follower's timeline from the followed user's posts; a notifier
 * would tell the followed user. In v1 nothing consumes this; it is follow's forward contract
 * (ADR-0002, CONTEXT-MAP).
 */
public record UserFollowed(UserId follower, UserId followed, Instant followedAt) {
}
