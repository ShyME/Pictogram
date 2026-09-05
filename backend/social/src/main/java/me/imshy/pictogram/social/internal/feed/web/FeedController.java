package me.imshy.pictogram.social.internal.feed.web;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.ApiPage;
import me.imshy.pictogram.shared.http.CurrentUser;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.social.internal.feed.FeedPost;
import me.imshy.pictogram.social.internal.feed.FeedQuery;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
class FeedController {

    private final FeedQuery feed;

    FeedController(FeedQuery feed) {
        this.feed = feed;
    }

    @ApiResponses({@ApiResponse(responseCode = "200", description = "A page of the viewer's feed, newest post first."),
        @ApiResponse(responseCode = "400", description = "The pagination cursor is malformed.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "The caller has no valid access token.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @GetMapping
    ApiPage<FeedPost> feed(@CurrentUser ViewerId viewer, @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "limit", required = false) Integer limit) {
        Cursor after = cursor == null ? null : Cursor.decode(cursor);
        FeedQuery.Page page = feed.pageFor(viewer, after, limit);
        return ApiPage.of(page.posts(), page.nextCursor());
    }
}
