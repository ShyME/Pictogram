package me.imshy.pictogram;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import me.imshy.pictogram.identity.IdentityStats;
import me.imshy.pictogram.post.PostStats;
import me.imshy.pictogram.social.SocialStats;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Product-level counts a dashboard can't get from HTTP/JVM meters alone (ADR-0016). Reads
// through each owning module's published stats interface, never its schema directly, so
// InternalSlicingTest and the module boundary stay intact. No per-user tags: cardinality
// stays flat regardless of user count.
//
// Gauge names end in ".count", not ".total": Prometheus's client reserves the "_total"
// suffix for counters and strips it from a gauge's wire name (it does NOT round-trip —
// "pictogram.posts.total" would render as "pictogram_posts"), which would silently
// collide "pictogram.users.total" onto the bare "pictogram_users" name.
@Component
class ProductStatsGaugeJob {

    private final IdentityStats identityStats;
    private final PostStats postStats;
    private final SocialStats socialStats;
    private final Clock clock;
    private final Duration activeWindow;

    private final AtomicLong usersTotal = new AtomicLong();
    private final AtomicLong usersActive = new AtomicLong();
    private final AtomicLong postsTotal = new AtomicLong();
    private final AtomicLong commentsTotal = new AtomicLong();
    private final AtomicLong likesTotal = new AtomicLong();
    private final AtomicLong followsTotal = new AtomicLong();

    ProductStatsGaugeJob(
            IdentityStats identityStats,
            PostStats postStats,
            SocialStats socialStats,
            MeterRegistry registry,
            Clock clock,
            ProductStatsProperties properties) {
        this.identityStats = identityStats;
        this.postStats = postStats;
        this.socialStats = socialStats;
        this.clock = clock;
        this.activeWindow = properties.activeWindow();

        registry.gauge("pictogram.users.count", usersTotal);
        registry.gauge("pictogram.users.active", usersActive);
        registry.gauge("pictogram.posts.count", postsTotal);
        registry.gauge("pictogram.comments.count", commentsTotal);
        registry.gauge("pictogram.likes.count", likesTotal);
        registry.gauge("pictogram.follows.count", followsTotal);

        // Otherwise every gauge reads 0 from startup until the cron first fires (up to
        // a
        // full publish-cron period later) — a scrape or alert in that window sees a
        // false
        // "no users/posts/etc" dip on every deploy.
        publish();
    }

    @Scheduled(cron = "${pictogram.stats.publish-cron}")
    void publish() {
        usersTotal.set(identityStats.totalUsers());
        usersActive.set(identityStats.activeSince(clock.instant().minus(activeWindow)));
        postsTotal.set(postStats.total());
        commentsTotal.set(socialStats.totalComments());
        likesTotal.set(socialStats.totalLikes());
        followsTotal.set(socialStats.totalFollows());
    }
}
