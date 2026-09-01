package me.imshy.pictogram.shared.http;

import java.util.List;

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
