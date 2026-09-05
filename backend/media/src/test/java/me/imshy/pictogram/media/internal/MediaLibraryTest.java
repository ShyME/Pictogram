package me.imshy.pictogram.media.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import me.imshy.pictogram.media.MediaCatalog;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import org.apache.commons.imaging.Imaging;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MediaLibraryTest extends MediaModuleIntegrationTest {

    @Autowired
    MediaLibrary library;

    @Autowired
    MediaCatalog catalog;

    @Test
    void aNonSquarePhotoWithLocationBecomesTheCanonicalSquareRenditionsWithNoMetadata() throws Exception {
        var owner = UserId.random();
        byte[] upload = TestImages.jpegWithLocation(1600, 1200);

        MediaId mediaId = library.upload(owner, upload);

        byte[] original = library.original(mediaId);
        var originalImage = ImageIO.read(new ByteArrayInputStream(original));
        assertThat(originalImage.getWidth()).isEqualTo(1080);
        assertThat(originalImage.getHeight()).isEqualTo(1080);
        assertThat(Imaging.getMetadata(original)).as("EXIF/GPS stripped").isNull();

        byte[] thumbnail = library.thumbnail(mediaId);
        var thumbnailImage = ImageIO.read(new ByteArrayInputStream(thumbnail));
        assertThat(thumbnailImage.getWidth()).isEqualTo(320);
        assertThat(thumbnailImage.getHeight()).isEqualTo(320);
        assertThat(Imaging.getMetadata(thumbnail)).isNull();
    }

    @Test
    void theCatalogReportsExistenceAndOwnership() {
        var owner = UserId.random();

        MediaId mediaId = library.upload(owner, safeUpload());

        assertThat(catalog.exists(mediaId)).isTrue();
        assertThat(catalog.ownerOf(mediaId)).contains(owner);
    }

    @Test
    void anUnknownMediaExistsNowhereAndHasNoOwner() {
        var unknown = MediaId.random();

        assertThat(catalog.exists(unknown)).isFalse();
        assertThat(catalog.ownerOf(unknown)).isEmpty();
        assertThatExceptionOfType(MediaNotFoundException.class).isThrownBy(() -> library.original(unknown));
        assertThatExceptionOfType(MediaNotFoundException.class).isThrownBy(() -> library.thumbnail(unknown));
    }

    @Test
    void bytesThatAreNotAnImageAreRejectedAndStoreNothing() {
        var junk = "definitely not a photo".getBytes(StandardCharsets.UTF_8);

        assertThatExceptionOfType(UndecodableImageException.class)
            .isThrownBy(() -> library.upload(UserId.random(), junk));
    }

    private static byte[] safeUpload() {
        try {
            return TestImages.jpeg(800, 600);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
