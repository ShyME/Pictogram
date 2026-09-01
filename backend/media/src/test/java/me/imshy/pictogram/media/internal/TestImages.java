package me.imshy.pictogram.media.internal;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

final class TestImages {

    private TestImages() {}

    static byte[] jpeg(int width, int height) throws Exception {
        return encode(solid(width, height), "jpeg");
    }

    static byte[] png(int width, int height) throws Exception {
        return encode(solid(width, height), "png");
    }

    static byte[] jpegWithLocation(int width, int height) throws Exception {
        return withExif(jpeg(width, height), 1, true);
    }

    static byte[] markedTopLeft(int width, int height, int orientation) throws Exception {
        BufferedImage image = solid(width, height);
        var g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width * 2 / 5, height * 2 / 5);
        g.dispose();
        return withExif(encode(image, "jpeg"), orientation, false);
    }

    private static BufferedImage solid(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(new Color(0x30, 0x60, 0x90));
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private static byte[] encode(BufferedImage image, String format) throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static byte[] withExif(byte[] jpeg, int orientation, boolean withGps) throws Exception {
        var outputSet = new TiffOutputSet();
        var root = outputSet.getOrCreateRootDirectory();
        root.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
        root.add(TiffTagConstants.TIFF_TAG_ORIENTATION, (short) orientation);
        outputSet.getOrCreateExifDirectory().add(TiffTagConstants.TIFF_TAG_SOFTWARE, "Pictogram test camera");
        if (withGps) {
            outputSet.setGpsInDegrees(-0.1257, 51.5085);
            outputSet.getGpsDirectory().add(GpsTagConstants.GPS_TAG_GPS_ALTITUDE, RationalNumber.valueOf(11));
        }
        var out = new ByteArrayOutputStream();
        new ExifRewriter().updateExifMetadataLossless(jpeg, out, outputSet);
        return out.toByteArray();
    }
}
