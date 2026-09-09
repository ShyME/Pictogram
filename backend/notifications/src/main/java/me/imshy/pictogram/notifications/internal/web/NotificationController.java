package me.imshy.pictogram.notifications.internal.web;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import me.imshy.pictogram.notifications.internal.NotificationQuery;
import me.imshy.pictogram.notifications.internal.NotificationView;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.ApiPage;
import me.imshy.pictogram.shared.http.CurrentUser;
import me.imshy.pictogram.shared.http.Cursor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
class NotificationController {

    private final NotificationQuery notifications;

    NotificationController(NotificationQuery notifications) {
        this.notifications = notifications;
    }

    record UnreadCount(long count) {
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A page of the caller's notifications, newest first."),
        @ApiResponse(responseCode = "400", description = "The pagination cursor is malformed.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "The caller has no valid access token.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @GetMapping
    ApiPage<NotificationView> notifications(@CurrentUser ViewerId viewer,
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "limit", required = false) Integer limit) {
        Cursor after = cursor == null ? null : Cursor.decode(cursor);
        NotificationQuery.Page page = notifications.pageFor(viewer, after, limit);
        return ApiPage.of(page.notifications(), page.nextCursor());
    }

    @ApiResponses({@ApiResponse(responseCode = "200", description = "The caller's unread notification count."),
        @ApiResponse(responseCode = "401", description = "The caller has no valid access token.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @GetMapping("/unread-count")
    UnreadCount unreadCount(@CurrentUser ViewerId viewer) {
        return new UnreadCount(notifications.unreadCountFor(viewer));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "204",
            description = "Every unread notification of the caller is now read (idempotent)."),
        @ApiResponse(responseCode = "401", description = "The caller has no valid access token.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @PostMapping("/mark-read")
    ResponseEntity<Void> markRead(@CurrentUser ViewerId viewer) {
        notifications.markAllReadFor(viewer);
        return ResponseEntity.noContent().build();
    }
}
