package me.imshy.pictogram.engagement.internal.web;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.imshy.pictogram.engagement.LikeCounts;
import me.imshy.pictogram.engagement.internal.Liking;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.BatchIds;
import me.imshy.pictogram.shared.http.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/engagement/likes")
class LikeController {

    private final Liking liking;
    private final LikeCounts counts;

    LikeController(Liking liking, LikeCounts counts) {
        this.liking = liking;
        this.counts = counts;
    }

    record PostLikesView(UUID postId, long likeCount, boolean likedByViewer) {

        static PostLikesView of(LikeCounts.PostLikes likes) {
            return new PostLikesView(likes.post().value(), likes.likeCount(), likes.likedByViewer());
        }
    }

    @ApiResponses(@ApiResponse(responseCode = "204", description = "The viewer now likes the post (or already did)."))
    @PutMapping("/{postId}")
    ResponseEntity<Void> like(@CurrentUser ViewerId viewer, @PathVariable("postId") UUID postId) {
        liking.like(viewer, new PostId(postId));
        return ResponseEntity.noContent().build();
    }

    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "The viewer no longer likes the post (or never did)."))
    @DeleteMapping("/{postId}")
    ResponseEntity<Void> unlike(@CurrentUser ViewerId viewer, @PathVariable("postId") UUID postId) {
        liking.unlike(viewer, new PostId(postId));
        return ResponseEntity.noContent().build();
    }

    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description =
                        "The like count for each requested post, plus the viewer's own like state when signed in.",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = PostLikesView.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "The request asked for more ids than the batch limit.",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(params = "postIds")
    List<PostLikesView> likesByPostIds(
            @CurrentUser Optional<ViewerId> viewer, @RequestParam("postIds") Set<UUID> postIds) {
        List<PostId> posts = BatchIds.checked(postIds).stream().map(PostId::new).toList();
        List<LikeCounts.PostLikes> likes = viewer.map(v -> counts.of(v, posts)).orElseGet(() -> counts.of(posts));
        return likes.stream().map(PostLikesView::of).toList();
    }
}
