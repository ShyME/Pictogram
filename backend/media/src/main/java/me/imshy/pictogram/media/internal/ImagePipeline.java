package me.imshy.pictogram.media.internal;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

/**
 * The re-encode step: an accepted upload in → one canonical square JPEG plus a square
 * thumbnail out (ADR-0006). Decoding to a raster and re-encoding is also what strips
 * EXIF/GPS — the pipeline reads the orientation tag first so the result is upright, then
 * nothing else from the source metadata survives. Anything that is not a JPEG, PNG or WebP
 * a codec can read — or whose pixel count is implausibly large for its byte size — raises
 * {@link UndecodableImageException}.
 *
 * <p>Stateless and free of I/O, so it is unit-tested directly; {@link MediaLibrary} is the
 * only caller.
 */
@Component
class ImagePipeline {

    static final int ORIGINAL_SIZE = 1080;
    static final int THUMBNAIL_SIZE = 320;

    private static final float JPEG_QUALITY = 0.82f;
    private static final Set<String> ACCEPTED_FORMATS = Set.of("jpeg", "jpg", "png", "webp");

    // A backstop against decompression bombs: the multipart size limit already caps the
    // bytes, so a file that still decodes to more than this many pixels is pathological.
    // Well clear of any real camera (a 108 MP phone sensor is ~1.1e8).
    private static final long MAX_PIXELS = 200_000_000L;

    record Renditions(byte[] original, byte[] thumbnail) {
    }

    Renditions transcode(byte[] upload) {
        BufferedImage upright = applyOrientation(decode(upload), orientationOf(upload));
        BufferedImage square = centreCrop(upright);
        return new Renditions(
                encodeJpeg(scaleTo(square, ORIGINAL_SIZE)),
                encodeJpeg(scaleTo(square, THUMBNAIL_SIZE)));
    }

    private static BufferedImage decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new UndecodableImageException();
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) {
                throw new UndecodableImageException();
            }
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new UndecodableImageException();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                if (!ACCEPTED_FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw new UndecodableImageException();
                }
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                    throw new UndecodableImageException();
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException brokenBytesOrCodec) {
            throw new UndecodableImageException();
        }
    }

    private static int orientationOf(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exif != null && exif.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
                return exif.getInt(ExifDirectoryBase.TAG_ORIENTATION);
            }
        } catch (Exception noReadableOrientation) {
            // no metadata, unreadable metadata, or no orientation tag — treat as upright
        }
        return 1;
    }

    /** Reorients per the EXIF orientation value (1–8); values 5–8 also swap width and height. */
    private static BufferedImage applyOrientation(BufferedImage src, int orientation) {
        if (orientation < 2 || orientation > 8) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        boolean swapAxes = orientation >= 5;
        AffineTransform transform = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
            default -> new AffineTransform();
        };

        BufferedImage dst = new BufferedImage(swapAxes ? h : w, swapAxes ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, transform, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage centreCrop(BufferedImage image) {
        int side = Math.min(image.getWidth(), image.getHeight());
        int left = (image.getWidth() - side) / 2;
        int top = (image.getHeight() - side) / 2;
        return image.getSubimage(left, top, side, side);
    }

    private static BufferedImage scaleTo(BufferedImage square, int size) {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(square, 0, 0, size, size, null);
        g.dispose();
        return scaled;
    }

    private static byte[] encodeJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        var out = new ByteArrayOutputStream();
        try (var stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            // Null metadata: the encoder writes a bare JFIF stream, so no EXIF/GPS block.
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode the canonical JPEG", e);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
