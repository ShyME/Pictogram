package me.imshy.pictogram.post.internal.web;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import me.imshy.pictogram.post.internal.PostDeletion;
import me.imshy.pictogram.post.internal.PostTimeline;
import me.imshy.pictogram.post.internal.PostView;
import me.imshy.pictogram.post.internal.Publishing;
import me.imshy.pictogram.post.internal.UnusableMediaException;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.ApiPage;
import me.imshy.pictogram.shared.http.CurrentUser;
import me.imshy.pictogram.shared.http.Cursor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
class PostsController {

    private final Publishing publishing;
    private final PostDeletion deletion;
    private final PostTimeline timeline;

    PostsController(Publishing publishing, PostDeletion deletion, PostTimeline timeline) {
        this.publishing = publishing;
        this.deletion = deletion;
        this.timeline = timeline;
    }

    record PublishPostRequest(UUID mediaId, String caption) {
    }

    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The post was published.",
                headers = @Header(name = "Location",
                        description = "The post's URL — id-based, live once a post-detail read lands.",
                        schema = @Schema(type = "string")),
                content = @Content(schema = @Schema(implementation = PostView.class))),
        @ApiResponse(responseCode = "400", description = "The caption is longer than 2200 characters.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422",
                description = "The image can't be used — no such media, or it belongs to someone else.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    ResponseEntity<PostView> publish(@CurrentUser UserId author, @RequestBody PublishPostRequest request) {
        MediaId mediaId = Optional.ofNullable(request.mediaId()).map(MediaId::new)
                .orElseThrow(UnusableMediaException::new);
        PostView post = publishing.publish(author, mediaId, request.caption());
        return ResponseEntity.created(URI.create("/api/posts/" + post.postId())).body(post);
    }

    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "The post was permanently deleted."),
        @ApiResponse(responseCode = "403",
                description = "The caller is not the author of this post.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "No post has that id.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{postId}")
    ResponseEntity<Void> delete(@CurrentUser UserId author, @PathVariable("postId") UUID postId) {
        deletion.delete(author, new PostId(postId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping(params = "author")
    ApiPage<PostView> timeline(@RequestParam("author") UUID author,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) Integer limit) {
        Cursor after = cursor == null ? null : Cursor.decode(cursor);
        PostTimeline.Page page = timeline.pageFor(new UserId(author), after, limit);
        return ApiPage.of(page.items(), page.nextCursor());
    }
}
