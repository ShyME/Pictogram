package me.imshy.pictogram.social.internal.comment.web;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.ApiPage;
import me.imshy.pictogram.shared.http.CurrentUser;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.social.internal.comment.CommentThread;
import me.imshy.pictogram.social.internal.comment.PostComment;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
class CommentController {

    private final CommentThread thread;

    CommentController(CommentThread thread) {
        this.thread = thread;
    }

    record CreateCommentRequest(String body) {
    }

    record CommentView(UUID commentId, UUID postId, UUID authorId, String body, Instant createdAt) {

        static CommentView of(PostComment comment) {
            return new CommentView(comment.commentId(), comment.postId().value(), comment.viewer().value(),
                comment.body(), comment.createdAt());
        }
    }

    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The comment was added.",
            headers = @Header(name = "Location", description = "The comment's URL.", schema = @Schema(type = "string")),
            content = @Content(schema = @Schema(implementation = CommentView.class))),
        @ApiResponse(responseCode = "400", description = "The comment is empty or longer than 1000 characters.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "The caller has no valid access token.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @PostMapping
    ResponseEntity<CommentView> add(@CurrentUser ViewerId viewer, @PathVariable("postId") UUID postId,
        @RequestBody CreateCommentRequest request) {
        CommentView view = CommentView.of(thread.comment(viewer, new PostId(postId), request.body()));
        return ResponseEntity.created(URI.create("/api/comments/" + view.commentId())).body(view);
    }

    @ApiResponses({@ApiResponse(responseCode = "200", description = "A page of the post's comments, oldest first."),
        @ApiResponse(responseCode = "400", description = "The pagination cursor is malformed.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @GetMapping
    ApiPage<CommentView> thread(@PathVariable("postId") UUID postId,
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "limit", required = false) Integer limit) {
        Cursor after = cursor == null ? null : Cursor.decode(cursor);
        CommentThread.Page page = thread.pageFor(new PostId(postId), after, limit);
        return ApiPage.of(page.comments().stream().map(CommentView::of).toList(), page.nextCursor());
    }
}
