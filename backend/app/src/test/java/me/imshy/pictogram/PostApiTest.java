package me.imshy.pictogram;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MockMvc;

@AppIntegrationTest
class PostApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void publishingNeedsAToken() throws Exception {
        mvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
    }

    @Test
    void anAuthorUploadsAPhotoPublishesItWithACaptionAndSeesItOnTheirGrid() throws Exception {
        var author = UUID.randomUUID().toString();
        String mediaId = uploadPhoto(author);

        mvc.perform(post("/api/posts")
                        .with(jwt().jwt(jwt -> jwt.subject(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":\"%s\",\"caption\":\"sunset over the bay\"}".formatted(mediaId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.postId").exists())
                .andExpect(jsonPath("$.authorId").value(author))
                .andExpect(jsonPath("$.mediaId").value(mediaId))
                .andExpect(jsonPath("$.caption").value("sunset over the bay"));

        mvc.perform(get("/api/posts").param("author", author))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].caption").value("sunset over the bay"))
                .andExpect(jsonPath("$.items[0].mediaId").value(mediaId))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void theGridIsServedWithoutAToken() throws Exception {
        var author = UUID.randomUUID().toString();
        publish(author, uploadPhoto(author), "a post");

        mvc.perform(get("/api/posts").param("author", author))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void aCaptionOverTheLimitIsABadRequestProblemDetail() throws Exception {
        var author = UUID.randomUUID().toString();
        String mediaId = uploadPhoto(author);

        mvc.perform(post("/api/posts")
                        .with(jwt().jwt(jwt -> jwt.subject(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":\"%s\",\"caption\":\"%s\"}".formatted(mediaId, "x".repeat(2201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "post-caption-too-long"));
    }

    @Test
    void publishingMediaTheCallerDoesNotOwnIsAnUnprocessableEntityProblemDetail() throws Exception {
        var owner = UUID.randomUUID().toString();
        var interloper = UUID.randomUUID().toString();
        String mediaId = uploadPhoto(owner);

        mvc.perform(post("/api/posts")
                        .with(jwt().jwt(jwt -> jwt.subject(interloper)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":\"%s\"}".formatted(mediaId)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "post-media-unusable"));
    }

    @Test
    void publishingAMediaIdNoMediaHasIsTheSameUnprocessableEntity() throws Exception {
        var author = UUID.randomUUID().toString();

        mvc.perform(post("/api/posts")
                        .with(jwt().jwt(jwt -> jwt.subject(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "post-media-unusable"));
    }

    @Test
    void publishingWithNoMediaIdIsAProblemDetailNotAServerError() throws Exception {
        var author = UUID.randomUUID().toString();

        mvc.perform(post("/api/posts")
                        .with(jwt().jwt(jwt -> jwt.subject(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caption\":\"no photo\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "post-media-unusable"));
    }

    @Test
    void publishingWithAMalformedMediaIdIsABadRequestNotAServerError() throws Exception {
        var author = UUID.randomUUID().toString();

        mvc.perform(post("/api/posts")
                        .with(jwt().jwt(jwt -> jwt.subject(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":\"not-a-uuid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingNeedsAToken() throws Exception {
        mvc.perform(delete("/api/posts/{postId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
    }

    @Test
    void anAuthorDeletesTheirOwnPostAndItLeavesTheirGrid() throws Exception {
        var author = UUID.randomUUID().toString();
        String postId = publish(author, uploadPhoto(author), "delete me");

        mvc.perform(delete("/api/posts/{postId}", postId).with(jwt().jwt(jwt -> jwt.subject(author))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/posts").param("author", author))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void deletingSomeoneElsesPostIsAForbiddenProblemDetail() throws Exception {
        var author = UUID.randomUUID().toString();
        var interloper = UUID.randomUUID().toString();
        String postId = publish(author, uploadPhoto(author), "not yours");

        mvc.perform(delete("/api/posts/{postId}", postId).with(jwt().jwt(jwt -> jwt.subject(interloper))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value(ProblemType.FORBIDDEN.uri().toString()));

        mvc.perform(get("/api/posts").param("author", author))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void deletingAPostThatDoesNotExistIsANotFoundProblemDetail() throws Exception {
        var author = UUID.randomUUID().toString();

        mvc.perform(delete("/api/posts/{postId}", UUID.randomUUID()).with(jwt().jwt(jwt -> jwt.subject(author))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemType.BASE + "post-not-found"));
    }

    private String uploadPhoto(String owner) throws Exception {
        var result = mvc.perform(multipart("/api/media").file(imagePart()).with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.mediaId");
    }

    private String publish(String author, String mediaId, String caption) throws Exception {
        var result = mvc.perform(post("/api/posts")
                        .with(jwt().jwt(jwt -> jwt.subject(author)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":\"%s\",\"caption\":\"%s\"}".formatted(mediaId, caption)))
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
