package me.imshy.pictogram.feed.internal.web;

import java.util.List;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.ApiPage;
import me.imshy.pictogram.shared.http.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
class FeedController {

    @GetMapping
    ApiPage<FeedCard> feed(@CurrentUser ViewerId viewer) {
        return ApiPage.lastPage(List.of());
    }
}
