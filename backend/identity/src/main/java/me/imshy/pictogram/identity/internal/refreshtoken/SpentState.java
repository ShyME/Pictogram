package me.imshy.pictogram.identity.internal.refreshtoken;

import java.time.Duration;
import java.time.Instant;

record SpentState(Instant consumedAt, Instant revokedAt) {

    boolean isBenignRaceWithin(Duration grace, Instant now) {
        return consumedAt != null
                && revokedAt == null
                && Duration.between(consumedAt, now).compareTo(grace) <= 0;
    }
}
