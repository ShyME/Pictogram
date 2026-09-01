package me.imshy.pictogram.media.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class OrphanCollectorTest extends MediaModuleIntegrationTest {

    private static final Duration GRACE = Duration.ofHours(24);

    @Autowired
    OrphanCollector collector;

    @Autowired
    MediaLibrary library;

    @Autowired
    BlobStore blobs;

    @MockitoBean
    Clock clock;

    final MutableClock time = MutableClock.at("2026-09-01T09:00:00Z");

    @BeforeEach
    void bindClock() {
        given(clock.instant()).willAnswer(invocation -> time.instant());
        given(clock.getZone()).willAnswer(invocation -> time.getZone());
        given(postReferences.referencedAmong(any())).willReturn(Set.of());
    }

    @Test
    void sweepsAwayEveryUnreferencedMediaPastTheGraceAndLeavesTheReferencedOneAlone() {
        MediaId livePostImage = library.upload(UserId.random(), jpeg());
        MediaId neverPosted = library.upload(UserId.random(), jpeg());
        MediaId hadItsPostDeleted = library.upload(UserId.random(), jpeg());
        given(postReferences.referencedAmong(any())).willReturn(Set.of(livePostImage));

        time.advance(GRACE.plusHours(1));
        int collected = collector.collectOrphans();

        assertThat(collected).isEqualTo(2);
        assertRemoved(neverPosted);
        assertRemoved(hadItsPostDeleted);
        assertThat(library.original(livePostImage)).isNotEmpty();
        assertThat(library.thumbnail(livePostImage)).isNotEmpty();
    }

    @Test
    void collectsAMediaOnceItsLastReferencingPostGoesAway() {
        MediaId image = library.upload(UserId.random(), jpeg());
        given(postReferences.referencedAmong(any())).willReturn(Set.of(image));
        time.advance(GRACE.plusHours(1));

        assertThat(collector.collectOrphans()).isZero();
        assertThat(library.original(image)).isNotEmpty();

        given(postReferences.referencedAmong(any())).willReturn(Set.of());

        assertThat(collector.collectOrphans()).isEqualTo(1);
        assertRemoved(image);
    }

    @Test
    void leavesMediaThatIsStillInsideTheGracePeriod() {
        MediaId fresh = library.upload(UserId.random(), jpeg());

        time.advance(GRACE.minusHours(1));
        int collected = collector.collectOrphans();

        assertThat(collected).isZero();
        assertThat(library.original(fresh)).isNotEmpty();
    }

    private void assertRemoved(MediaId mediaId) {
        assertThatExceptionOfType(MediaNotFoundException.class).isThrownBy(() -> library.original(mediaId));
        assertThatExceptionOfType(RuntimeException.class)
                .as("bytes for %s are gone from storage too", mediaId)
                .isThrownBy(() -> blobs.get(StorageKeys.original(mediaId)));
    }

    private static byte[] jpeg() {
        try {
            return TestImages.jpeg(800, 600);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
