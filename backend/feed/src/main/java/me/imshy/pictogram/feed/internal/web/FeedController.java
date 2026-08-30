package me.imshy.pictogram.feed.internal.web;

import java.util.List;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.ApiPage;
import me.imshy.pictogram.shared.http.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The viewer's home view. Fan-out-on-read assembly from {@code follow} and {@code post}
 * (ADR-0003) arrives with a later slice; until there are posts to assemble, every viewer's
 * feed is a single empty page — enough for a freshly onboarded user to land somewhere.
 */
@RestController
@RequestMapping("/api/feed")
class FeedController {

    @GetMapping
    ApiPage<FeedCard> feed(@CurrentUser ViewerId viewer) {
        // viewer will select the follow graph once assembly lands; for now it only pins the
        // endpoint as authenticated and viewer-scoped rather than anonymous.
        return ApiPage.lastPage(List.of());
    }
}
