package me.imshy.pictogram.social.internal.feed.web;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import me.imshy.pictogram.shared.http.ApiPage;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.social.internal.feed.ExploreQuery;
import me.imshy.pictogram.social.internal.feed.FeedPost;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/explore")
class ExploreController {

    private final ExploreQuery explore;

    ExploreController(ExploreQuery explore) {
        this.explore = explore;
    }

    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "A page of every post newest first, across every author, no sign-in needed."),
        @ApiResponse(
                responseCode = "400",
                description = "The pagination cursor is malformed.",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    ApiPage<FeedPost> explore(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) Integer limit) {
        Cursor after = cursor == null ? null : Cursor.decode(cursor);
        ExploreQuery.Page page = explore.pageFor(after, limit);
        return ApiPage.of(page.posts(), page.nextCursor());
    }
}
