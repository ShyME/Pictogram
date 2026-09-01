package me.imshy.pictogram.follow.internal.web;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import me.imshy.pictogram.follow.FollowGraph;
import me.imshy.pictogram.follow.internal.FollowList;
import me.imshy.pictogram.follow.internal.FollowRelationships;
import me.imshy.pictogram.follow.internal.Following;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.ApiPage;
import me.imshy.pictogram.shared.http.BatchIds;
import me.imshy.pictogram.shared.http.CurrentUser;
import me.imshy.pictogram.shared.http.Cursor;
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
@RequestMapping("/api/follows")
class FollowController {

    private final Following following;
    private final FollowGraph graph;
    private final FollowList lists;
    private final FollowRelationships relationships;

    FollowController(Following following, FollowGraph graph, FollowList lists, FollowRelationships relationships) {
        this.following = following;
        this.graph = graph;
        this.lists = lists;
        this.relationships = relationships;
    }

    record FollowRelationship(long followerCount, long followingCount, boolean followedByViewer) {
    }

    record FollowRelationshipView(UUID userId, long followerCount, long followingCount, boolean followedByViewer) {

        static FollowRelationshipView of(FollowRelationships.Relationship relationship) {
            return new FollowRelationshipView(relationship.user().value(), relationship.followerCount(),
                    relationship.followingCount(), relationship.followedByViewer());
        }
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

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The viewer's follow standing with each requested user.",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = FollowRelationshipView.class)))),
        @ApiResponse(responseCode = "400", description = "The request asked for more ids than the batch limit.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "The caller has no valid access token.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(params = "ids")
    List<FollowRelationshipView> relationshipsByIds(@CurrentUser ViewerId viewer, @RequestParam("ids") Set<UUID> ids) {
        List<UserId> users = BatchIds.checked(ids).stream().map(UserId::new).toList();
        return relationships.of(viewer, users).stream().map(FollowRelationshipView::of).toList();
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A page of the users who follow this user, newest follow first."),
        @ApiResponse(responseCode = "401", description = "The caller has no valid access token.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{userId}/followers")
    ApiPage<UUID> followers(@CurrentUser ViewerId viewer, @PathVariable("userId") UUID userId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return listPage(lists.followersOf(new UserId(userId), decode(cursor), limit));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A page of the users this user follows, newest follow first."),
        @ApiResponse(responseCode = "401", description = "The caller has no valid access token.",
                content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{userId}/following")
    ApiPage<UUID> following(@CurrentUser ViewerId viewer, @PathVariable("userId") UUID userId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return listPage(lists.followingOf(new UserId(userId), decode(cursor), limit));
    }

    private static Cursor decode(String cursor) {
        return cursor == null ? null : Cursor.decode(cursor);
    }

    private static ApiPage<UUID> listPage(FollowList.Page page) {
        return ApiPage.of(page.items().stream().map(UserId::value).toList(), page.nextCursor());
    }
}
