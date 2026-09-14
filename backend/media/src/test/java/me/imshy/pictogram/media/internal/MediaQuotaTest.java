package me.imshy.pictogram.media.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class MediaQuotaTest extends MediaModuleIntegrationTest {

    @Autowired
    MediaLibrary library;

    @Autowired
    Medias medias;

    @DynamicPropertySource
    static void quota(DynamicPropertyRegistry registry) {
        registry.add("pictogram.media.quota.max-bytes-per-user", () -> "1B");
    }

    @Test
    void rejectsAnUploadThatWouldExceedTheOwnersQuota() throws Exception {
        var owner = UserId.random();

        assertThatExceptionOfType(MediaQuotaExceededException.class)
            .isThrownBy(() -> library.upload(owner, TestImages.jpeg(800, 600)));
    }

    @Test
    void aRejectedUploadStoresNothing() throws Exception {
        var owner = UserId.random();

        assertThatExceptionOfType(MediaQuotaExceededException.class)
            .isThrownBy(() -> library.upload(owner, TestImages.jpeg(800, 600)));

        assertThat(medias.totalBytesForOwner(owner.value())).isZero();
    }
}
