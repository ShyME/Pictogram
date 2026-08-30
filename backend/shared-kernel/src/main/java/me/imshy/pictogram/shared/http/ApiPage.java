package me.imshy.pictogram.shared.http;

import java.util.List;

/**
 * The pagination envelope every list endpoint returns: the page of {@code items} and an
 * opaque {@code nextCursor}, or {@code null} on the last page (spec §API). Build it with
 * {@link #of} from a domain {@link Cursor} so the encoding stays in one place.
 */
public record ApiPage<T>(List<T> items, String nextCursor) {

    public ApiPage {
        items = List.copyOf(items);
    }

    public static <T> ApiPage<T> of(List<T> items, Cursor nextCursor) {
        return new ApiPage<>(items, nextCursor == null ? null : nextCursor.encode());
    }

    public static <T> ApiPage<T> lastPage(List<T> items) {
        return new ApiPage<>(items, null);
    }
}
