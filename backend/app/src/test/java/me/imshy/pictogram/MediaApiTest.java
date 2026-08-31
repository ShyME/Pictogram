package me.imshy.pictogram;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.sql.DataSource;
import me.imshy.pictogram.shared.http.ProblemType;
import me.imshy.pictogram.testsupport.DatabaseCleaner;
import me.imshy.pictogram.testsupport.SharedMinio;
import me.imshy.pictogram.testsupport.SharedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The media endpoints through the real security chain: uploading needs a token, but the
 * rendition bytes are served to anyone (they back public profile grids and feed cards —
 * spec stories 13, 18, 35). A non-image upload and an unknown id are Problem Details.
 */
@SpringBootTest(classes = PictogramApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DataSource dataSource;

    @DynamicPropertySource
    static void infra(DynamicPropertyRegistry registry) {
        SharedPostgres.registerTo(registry);
        SharedMinio.registerTo(registry);
    }

    @AfterEach
    void truncateAllTables() {
        new DatabaseCleaner(dataSource).truncateAll();
    }

    @Test
    void uploadingNeedsAToken() throws Exception {
        mvc.perform(multipart("/api/media").file(imagePart()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
    }

    @Test
    void aSignedInUserUploadsAnImageAndCanFetchBothRenditionsAnonymously() throws Exception {
        var user = UUID.randomUUID().toString();

        var upload = mvc.perform(multipart("/api/media").file(imagePart())
                        .with(jwt().jwt(jwt -> jwt.subject(user))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaId").exists())
                .andExpect(header().exists("Location"))
                .andReturn();

        String mediaId = JsonPath.read(upload.getResponse().getContentAsString(), "$.mediaId");

        mvc.perform(get("/api/media/{id}/original", mediaId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE));

        mvc.perform(get("/api/media/{id}/thumbnail", mediaId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE));
    }

    @Test
    void bytesThatAreNotAnImageAreABadRequestProblemDetail() throws Exception {
        var notAnImage = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        mvc.perform(multipart("/api/media").file(notAnImage)
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "media-undecodable"));
    }

    @Test
    void anUnknownMediaIdIsANotFoundProblemDetail() throws Exception {
        mvc.perform(get("/api/media/{id}/original", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "media-not-found"));
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
