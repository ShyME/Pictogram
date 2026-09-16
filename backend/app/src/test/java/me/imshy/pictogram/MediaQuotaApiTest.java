package me.imshy.pictogram;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import me.imshy.pictogram.shared.http.ProblemType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AppIntegrationTest
class MediaQuotaApiTest {

    @Autowired
    MockMvc mvc;

    @DynamicPropertySource
    static void tinyQuota(DynamicPropertyRegistry registry) {
        registry.add("pictogram.media.quota.max-bytes-per-user", () -> "1B");
    }

    @Test
    void anUploadThatWouldExceedTheQuotaIsAPayloadTooLargeProblemDetail() throws Exception {
        mvc.perform(
            multipart("/api/media").file(imagePart()).with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
            .andExpect(status().is(413)).andExpect(jsonPath("$.type").value(ProblemType.BASE + "media-quota-exceeded"));
    }

    private static MockMultipartFile imagePart() throws Exception {
        var image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(new Color(0x22, 0x88, 0x44));
        g.fillRect(0, 0, 1200, 800);
        g.dispose();
        var out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", out);
        return new MockMultipartFile("file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, out.toByteArray());
    }
}
