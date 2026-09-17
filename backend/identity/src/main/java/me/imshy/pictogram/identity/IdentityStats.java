package me.imshy.pictogram.identity;

import java.time.Instant;

public interface IdentityStats {

    long totalUsers();

    long activeSince(Instant since);
}
