package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import me.imshy.pictogram.shared.http.ProblemType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AppIntegrationTest
class ExploreApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void needsNoTokenAtAll() throws Exception {
        mvc.perform(get("/api/explore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void mergesPostsFromEveryAuthorRegardlessOfWhoTheCallerFollows() throws Exception {
        var ada = UUID.randomUUID().toString();
        var bob = UUID.randomUUID().toString();
        String adasPost = publish(ada, uploadPhoto(ada));
        String bobsPost = publish(bob, uploadPhoto(bob));

        String body = mvc.perform(get("/api/explore"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(JsonPath.<List<String>>read(body, "$.items[*].postId")).contains(adasPost, bobsPost);
    }

    @Test
    void aMalformedCursorIsRejected() throws Exception {
        mvc.perform(get("/api/explore?cursor=not-a-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value(ProblemType.INVALID_CURSOR.uri().toString()));
    }

    @Test
    void aSignedInCallerSeesTheSameGlobalPageAsAnAnonymousOne() throws Exception {
        var author = UUID.randomUUID().toString();
        String postId = publish(author, uploadPhoto(author));

        mvc.perform(get("/api/explore")
                        .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].postId").value(Matchers.hasItem(postId)));
    }

    private String uploadPhoto(String owner) throws Exception {
        var result = mvc.perform(multipart("/api/media").file(imagePart()).with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.mediaId");
    }

    private String publish(String author, String mediaId) throws Exception {
        var result = mvc.perform(post("/api/posts")
                        .with(jwt().jwt(jwt -> jwt.subject(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":\"%s\"}".formatted(mediaId)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.postId");
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
