package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cross-actor coverage for the owned/mutating endpoints that
 * {@link PostApiTest} (post delete) and {@link CommentApiTest} (comment delete)
 * don't already exercise, plus media ownership at publish time, also in
 * {@link PostApiTest}: a stranger's like/follow/comment never lands as, or
 * displaces, another caller's; and profile writes have no route but
 * {@code /me}.
 */
@AppIntegrationTest
class CrossActorAuthorizationApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void unlikingAsAStrangerLeavesTheOriginalLikersLikeInPlace() throws Exception {
        var ada = UUID.randomUUID().toString();
        var stranger = UUID.randomUUID().toString();
        var post = UUID.randomUUID().toString();

        mvc.perform(put("/api/likes/" + post).with(jwt().jwt(jwt -> jwt.subject(ada))))
            .andExpect(status().isNoContent());

        mvc.perform(delete("/api/likes/" + post).with(jwt().jwt(jwt -> jwt.subject(stranger))))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/likes").param("postIds", post).with(jwt().jwt(jwt -> jwt.subject(ada))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].likeCount").value(1))
            .andExpect(jsonPath("$[0].likedByViewer").value(true));
    }

    @Test
    void unfollowingAsAStrangerLeavesTheOriginalFollowersFollowInPlace() throws Exception {
        var ada = UUID.randomUUID().toString();
        var stranger = UUID.randomUUID().toString();
        var target = UUID.randomUUID();

        mvc.perform(put("/api/follows/" + target).with(jwt().jwt(jwt -> jwt.subject(ada))))
            .andExpect(status().isNoContent());

        mvc.perform(delete("/api/follows/" + target).with(jwt().jwt(jwt -> jwt.subject(stranger))))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/follows/" + target).with(jwt().jwt(jwt -> jwt.subject(ada)))).andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(1)).andExpect(jsonPath("$.followedByViewer").value(true));
    }

    @Test
    void commentingOnAStrangersPostAttributesTheCommentToTheCommenterNotThePostAuthor() throws Exception {
        var postAuthor = UUID.randomUUID().toString();
        var commenter = UUID.randomUUID().toString();
        String post = publish(postAuthor, uploadPhoto(postAuthor));

        var created = mvc
            .perform(post("/api/posts/" + post + "/comments").with(jwt().jwt(jwt -> jwt.subject(commenter)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"not yours to caption\"}"))
            .andExpect(status().isCreated()).andReturn();

        String authorId = JsonPath.read(created.getResponse().getContentAsString(), "$.authorId");
        assertThat(authorId).isEqualTo(commenter).isNotEqualTo(postAuthor);
    }

    @Test
    void onboardingTwoDifferentCallersCreatesTwoIndependentProfiles() throws Exception {
        var ada = UUID.randomUUID().toString();
        var bob = UUID.randomUUID().toString();
        var adaUsername = "ada_" + ada.substring(0, 8);
        var bobUsername = "bob_" + bob.substring(0, 8);

        mvc.perform(post("/api/profiles").with(jwt().jwt(jwt -> jwt.subject(ada)))
            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"%s\"}".formatted(adaUsername)))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/profiles").with(jwt().jwt(jwt -> jwt.subject(bob)))
            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"%s\"}".formatted(bobUsername)))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/profiles/me").with(jwt().jwt(jwt -> jwt.subject(ada)))).andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(adaUsername));
        mvc.perform(get("/api/profiles/me").with(jwt().jwt(jwt -> jwt.subject(bob)))).andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(bobUsername));
    }

    @Test
    void thereIsNoRouteToWriteAnotherCallersProfileByIdOnlyMe() throws Exception {
        var stranger = UUID.randomUUID().toString();

        mvc.perform(put("/api/profiles/" + UUID.randomUUID()).with(jwt().jwt(jwt -> jwt.subject(stranger)))
            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"whoever\"}"))
            .andExpect(status().is4xxClientError());
    }

    private String uploadPhoto(String owner) throws Exception {
        var result = mvc.perform(multipart("/api/media").file(imagePart()).with(jwt().jwt(jwt -> jwt.subject(owner))))
            .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.mediaId");
    }

    private String publish(String author, String mediaId) throws Exception {
        var result = mvc
            .perform(post("/api/posts").with(jwt().jwt(jwt -> jwt.subject(author)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"mediaId\":\"%s\"}".formatted(mediaId)))
            .andExpect(status().isCreated()).andReturn();
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
