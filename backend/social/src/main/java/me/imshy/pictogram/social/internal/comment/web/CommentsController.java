package me.imshy.pictogram.social.internal.comment.web;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.BatchIds;
import me.imshy.pictogram.shared.http.CurrentUser;
import me.imshy.pictogram.social.CommentCounts;
import me.imshy.pictogram.social.internal.comment.CommentThread;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
class CommentsController {

    private final CommentCounts counts;
    private final CommentThread thread;

    CommentsController(CommentCounts counts, CommentThread thread) {
        this.counts = counts;
        this.thread = thread;
    }

    record PostCommentsView(UUID postId, long commentCount) {

        static PostCommentsView of(CommentCounts.PostComments comments) {
            return new PostCommentsView(comments.post().value(), comments.commentCount());
        }
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The comment count for each requested post.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PostCommentsView.class)))),
        @ApiResponse(responseCode = "400", description = "The request asked for more ids than the batch limit.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @GetMapping(params = "postIds")
    List<PostCommentsView> countsByPostIds(@RequestParam("postIds") Set<UUID> postIds) {
        List<PostId> posts = BatchIds.checked(postIds).stream().map(PostId::new).toList();
        return counts.of(posts).stream().map(PostCommentsView::of).toList();
    }

    @ApiResponses({
        @ApiResponse(responseCode = "204",
            description = "The comment is gone (or was never there — the delete is idempotent)."),
        @ApiResponse(responseCode = "403",
            description = "The caller is neither the comment's author nor the post's author.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @DeleteMapping("/{commentId}")
    ResponseEntity<Void> deleteComment(@CurrentUser ViewerId viewer, @PathVariable("commentId") UUID commentId) {
        thread.delete(viewer, commentId);
        return ResponseEntity.noContent().build();
    }
}
