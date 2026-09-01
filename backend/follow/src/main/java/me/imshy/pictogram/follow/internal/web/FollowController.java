package me.imshy.pictogram.follow.internal.web;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Optional;
import java.util.UUID;
import me.imshy.pictogram.follow.FollowGraph;
import me.imshy.pictogram.follow.internal.Following;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The follow endpoints, one relationship resource per followed user
 * ({@code /api/follows/{userId}}). A signed-in viewer {@code PUT}s to follow and
 * {@code DELETE}s to unfollow — both idempotent, both {@code 204}, both acting as the
 * caller; a self-follow is a {@code 422} Problem Detail. {@code GET} is public: it returns
 * the followed user's follower / following counts (a profile page is shareable by link —
 * spec story 18) plus, for a signed-in viewer only, whether they follow that user.
 */
@RestController
@RequestMapping("/api/follows")
class FollowController {

    private final Following following;
    private final FollowGraph graph;

    FollowController(Following following, FollowGraph graph) {
        this.following = following;
        this.graph = graph;
    }

    /**
     * A user's follow standing. {@code followedByViewer} is {@code false} for an anonymous
     * caller — the counts are public, the relationship is the viewer's own.
     */
    record FollowRelationship(long followerCount, long followingCount, boolean followedByViewer) {
    }

    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "The viewer now follows the user (or already did)."),
        @ApiResponse(responseCode = "422", description = "The viewer tried to follow themselves.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{userId}")
    ResponseEntity<Void> follow(@CurrentUser ViewerId viewer, @PathVariable("userId") UUID userId) {
        following.follow(viewer, new UserId(userId));
        return ResponseEntity.noContent().build();
    }

    @ApiResponses(
        @ApiResponse(responseCode = "204", description = "The viewer no longer follows the user (or never did).")
    )
    @DeleteMapping("/{userId}")
    ResponseEntity<Void> unfollow(@CurrentUser ViewerId viewer, @PathVariable("userId") UUID userId) {
        following.unfollow(viewer, new UserId(userId));
        return ResponseEntity.noContent().build();
    }

    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "The user's follower / following counts and the viewer's relationship.",
                content = @Content(schema = @Schema(implementation = FollowRelationship.class)))
    )
    @GetMapping("/{userId}")
    FollowRelationship relationship(@CurrentUser Optional<ViewerId> viewer, @PathVariable("userId") UUID userId) {
        UserId followed = new UserId(userId);
        boolean followedByViewer = viewer.map(v -> graph.isFollowing(v, followed)).orElse(false);
        return new FollowRelationship(
                graph.followerCount(followed), graph.followingCount(followed), followedByViewer);
    }
}
