package me.imshy.pictogram.scenario;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/**
 * The seam every {@code *Scenarios} mixin is written against: a {@link PictogramApi} to act through,
 * and a throwaway photo to upload. {@code InProcessScenarioTest} binds {@link #pictogram()} to the
 * {@link InProcessDriver}; {@code BlackboxScenarioTest} binds it to the {@link ContainerDriver}. The
 * scenario bodies never see which.
 */
interface PictogramScenario {

    PictogramApi pictogram();

    /** A valid but content-free JPEG — the media pipeline only cares that it decodes. */
    default byte[] jpegPhoto() {
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
