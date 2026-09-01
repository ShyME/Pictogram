package me.imshy.pictogram.feed.internal;

import java.util.List;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.Cursor;

public interface FeedQuery {

    Page pageFor(ViewerId viewer, Cursor after, Integer limit);

    record Page(List<FeedPost> posts, Cursor nextCursor) {
    }
}
