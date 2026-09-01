package me.imshy.pictogram.media.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.apache.commons.imaging.Imaging;
import org.junit.jupiter.api.Test;

class ImagePipelineTest {

    private final ImagePipeline pipeline = new ImagePipeline();

    @Test
    void bytesThatAreNotAnImageAreRejected() {
        var junk = "this is not an image".getBytes(StandardCharsets.UTF_8);

        assertThatExceptionOfType(UndecodableImageException.class).isThrownBy(() -> pipeline.transcode(junk));
    }

    @Test
    void anEmptyUploadIsRejected() {
        assertThatExceptionOfType(UndecodableImageException.class).isThrownBy(() -> pipeline.transcode(new byte[0]));
    }

    @Test
    void aDecodableImageInAFormatWeDoNotAcceptIsRejected() throws Exception {
        var gif = new java.io.ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "gif", gif);

        assertThatExceptionOfType(UndecodableImageException.class)
                .isThrownBy(() -> pipeline.transcode(gif.toByteArray()));
    }

    @Test
    void theWebpReaderPluginIsOnTheClasspath() {
        assertThat(ImageIO.getImageReadersByFormatName("webp").hasNext())
                .as("imageio-webp must be registered for WebP uploads to decode")
                .isTrue();
    }

    @Test
    void theOriginalIsAJpegAtTheOneCanonicalSquareSize() throws Exception {
        var renditions = pipeline.transcode(TestImages.jpeg(1600, 900));

        var original = ImageIO.read(new ByteArrayInputStream(renditions.original()));
        assertThat(original.getWidth()).isEqualTo(ImagePipeline.ORIGINAL_SIZE);
        assertThat(original.getHeight()).isEqualTo(ImagePipeline.ORIGINAL_SIZE);
        assertThat(formatOf(renditions.original())).isEqualTo("JPEG");
    }

    @Test
    void theThumbnailIsAJpegAtTheThumbnailSquareSize() throws Exception {
        var renditions = pipeline.transcode(TestImages.png(800, 1200));

        var thumbnail = ImageIO.read(new ByteArrayInputStream(renditions.thumbnail()));
        assertThat(thumbnail.getWidth()).isEqualTo(ImagePipeline.THUMBNAIL_SIZE);
        assertThat(thumbnail.getHeight()).isEqualTo(ImagePipeline.THUMBNAIL_SIZE);
        assertThat(formatOf(renditions.thumbnail())).isEqualTo("JPEG");
    }

    @Test
    void exifAndGpsMetadataOnTheUploadAreGoneFromBothRenditions() throws Exception {
        byte[] withLocation = TestImages.jpegWithLocation(1600, 1200);
        assertThat(Imaging.getMetadata(withLocation)).isNotNull();

        var renditions = pipeline.transcode(withLocation);

        assertThat(Imaging.getMetadata(renditions.original())).isNull();
        assertThat(Imaging.getMetadata(renditions.thumbnail())).isNull();
    }

    @Test
    void theCanonicalRenditionIsUprightWhenTheUploadCarriesAnOrientationTag() throws Exception {
        byte[] rotated = TestImages.markedTopLeft(900, 600, 6);

        var original = ImageIO.read(
                new ByteArrayInputStream(pipeline.transcode(rotated).original()));

        assertThat(brightnessTopRight(original)).isLessThan(brightnessTopLeft(original));
    }

    @Test
    void anUploadWithNoMeaningfulOrientationTagIsLeftAsItIs() throws Exception {
        byte[] plain = TestImages.markedTopLeft(900, 600, 1);

        var original =
                ImageIO.read(new ByteArrayInputStream(pipeline.transcode(plain).original()));

        assertThat(brightnessTopLeft(original)).isLessThan(brightnessTopRight(original));
    }

    private static double brightnessTopLeft(BufferedImage image) {
        return brightnessAt(image, image.getWidth() / 4, image.getHeight() / 4);
    }

    private static double brightnessTopRight(BufferedImage image) {
        return brightnessAt(image, image.getWidth() * 3 / 4, image.getHeight() / 4);
    }

    private static double brightnessAt(BufferedImage image, int x, int y) {
        Color c = new Color(image.getRGB(x, y));
        return c.getRed() + c.getGreen() + c.getBlue();
    }

    private static String formatOf(byte[] bytes) throws Exception {
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            return readers.hasNext() ? readers.next().getFormatName().toUpperCase() : "UNKNOWN";
        }
    }
}
