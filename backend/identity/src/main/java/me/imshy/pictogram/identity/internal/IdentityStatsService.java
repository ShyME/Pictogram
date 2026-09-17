package me.imshy.pictogram.identity.internal;

import java.time.Instant;
import me.imshy.pictogram.identity.IdentityStats;
import me.imshy.pictogram.identity.internal.refreshtoken.RefreshTokens;
import org.springframework.stereotype.Service;

@Service
class IdentityStatsService implements IdentityStats {

    private final AppUsers appUsers;
    private final RefreshTokens refreshTokens;

    IdentityStatsService(AppUsers appUsers, RefreshTokens refreshTokens) {
        this.appUsers = appUsers;
        this.refreshTokens = refreshTokens;
    }

    @Override
    public long totalUsers() {
        return appUsers.count();
    }

    @Override
    public long activeSince(Instant since) {
        return refreshTokens.countDistinctUsersActiveSince(since);
    }
}
