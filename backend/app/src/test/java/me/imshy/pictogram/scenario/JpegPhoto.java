package me.imshy.pictogram.scenario;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

public class JpegPhoto {
    static byte[] some() {
        try {
            var image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics();
            g.setColor(new Color(0x33, 0x66, 0x99));
            g.fillRect(0, 0, 1200, 800);
            g.dispose();
            var out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
