package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import me.imshy.pictogram.identity.IdentityStats;
import me.imshy.pictogram.post.PostStats;
import me.imshy.pictogram.social.SocialStats;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// The job is a thin composition over each module's published stats interface (ADR-0016) —
// their own counting logic is pinned by PostStatsContractTest / SocialStatsContractTest /
// IdentityStatsServiceTest. This pins the composition: gauge names, values, and the
// active-window cutoff. A plain unit test, not a Spring context — the job takes only these
// three published interfaces and a MeterRegistry, none of which need real wiring to verify.
//
// Every test constructs its own job (after stubbing, not before): the constructor itself
// calls publish() so a gauge is never stuck at 0 between startup and the first cron tick.
class ProductStatsGaugeJobTest {

    private final IdentityStats identityStats = mock(IdentityStats.class);
    private final PostStats postStats = mock(PostStats.class);
    private final SocialStats socialStats = mock(SocialStats.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC);

    // Kept alive for the test's duration: MeterRegistry.gauge() holds its state
    // object
    // (the job's AtomicLong fields) by weak reference, so an unreferenced job is
    // free to be
    // GC'd between construction and assertion, silently turning every gauge to NaN.
    private ProductStatsGaugeJob job;

    @Test
    void publishesEveryGaugeFromItsOwningModulesStats() {
        given(identityStats.totalUsers()).willReturn(11L);
        given(identityStats.activeSince(any())).willReturn(4L);
        given(postStats.total()).willReturn(23L);
        given(socialStats.totalComments()).willReturn(7L);
        given(socialStats.totalLikes()).willReturn(31L);
        given(socialStats.totalFollows()).willReturn(9L);

        job = job();
        job.publish();

        assertThat(gauge("pictogram.users.count")).isEqualTo(11);
        assertThat(gauge("pictogram.users.active")).isEqualTo(4);
        assertThat(gauge("pictogram.posts.count")).isEqualTo(23);
        assertThat(gauge("pictogram.comments.count")).isEqualTo(7);
        assertThat(gauge("pictogram.likes.count")).isEqualTo(31);
        assertThat(gauge("pictogram.follows.count")).isEqualTo(9);
    }

    @Test
    void everyGaugeIsPopulatedAtConstructionNotOnlyAfterTheFirstScheduledPublish() {
        given(postStats.total()).willReturn(5L);

        job = job();

        // Otherwise a scrape right after startup would see every gauge as 0 until the
        // cron first fires, up to a full publish-cron period later.
        assertThat(gauge("pictogram.posts.count")).isEqualTo(5);
    }

    @Test
    void activeUsersAreQueriedSinceTheConfiguredActiveWindow() {
        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);

        job = job();

        verify(identityStats).activeSince(since.capture());
        assertThat(since.getValue()).isCloseTo(clock.instant().minus(Duration.ofHours(24)),
            within(0, ChronoUnit.SECONDS));
    }

    private ProductStatsGaugeJob job() {
        return new ProductStatsGaugeJob(identityStats, postStats, socialStats, registry, clock,
            new ProductStatsProperties(Duration.ofHours(24)));
    }

    private double gauge(String name) {
        return registry.get(name).gauge().value();
    }
}
