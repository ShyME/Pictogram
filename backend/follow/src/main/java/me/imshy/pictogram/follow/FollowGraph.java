package me.imshy.pictogram.follow;

import java.util.List;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;

/**
 * follow's published interface (spec §follow): the questions the follow graph itself can
 * answer, for other contexts to read synchronously (ADR-0002 — queries only, never a
 * command). {@code feed} asks {@link #usersFollowedBy} to assemble a viewer's page
 * (ADR-0003, fan-out-on-read); the SPA reads the counts and {@link #isFollowing} to render
 * a profile page (ADR-0005, client-side composition).
 *
 * <p>No follower or following <em>lists</em> — v1 exposes counts only (spec story 41).
 */
public interface FollowGraph {

    /** The users this viewer currently follows, in no particular order. */
    List<UserId> usersFollowedBy(ViewerId viewer);

    /** How many users follow this user. */
    long followerCount(UserId user);

    /** How many users this user follows. */
    long followingCount(UserId user);

    /** Whether this viewer currently follows this user. */
    boolean isFollowing(ViewerId viewer, UserId user);
}
