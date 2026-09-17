package me.imshy.pictogram.shared.http;

import me.imshy.pictogram.shared.UserId;

/**
 * A per-user, per-action write-path throttle. Any mutating endpoint can call it
 * — it is not welded to one feature or bounded context.
 */
public interface RateLimiter {

    /**
     * @throws RateLimitExceededException
     *             if {@code userId} has already used up its permits for
     *             {@code action} in the current window.
     */
    void requirePermit(UserId userId, String action);
}
