package me.imshy.pictogram.identity.internal.refreshtoken;

import java.time.Duration;
import java.time.Instant;

/**
 * The spent timestamps of a refresh-token row. A second presentation of an already-consumed
 * token is a benign concurrent refresh only within {@code grace} of the consume and while the
 * family is still live. Family revocation stamps {@code revokedAt} on consumed rows too, so a
 * non-null {@code revokedAt} is always outside the grace (ADR-0004).
 */
record SpentState(Instant consumedAt, Instant revokedAt) {

    boolean isBenignRaceWithin(Duration grace, Instant now) {
        return consumedAt != null
                && revokedAt == null
                && Duration.between(consumedAt, now).compareTo(grace) <= 0;
    }
}
