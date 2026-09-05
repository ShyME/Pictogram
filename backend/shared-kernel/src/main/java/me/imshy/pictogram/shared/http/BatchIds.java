package me.imshy.pictogram.shared.http;

import java.util.Set;

public final class BatchIds {

    public static final int MAX = 100;

    private BatchIds() {
    }

    public static <T> Set<T> checked(Set<T> ids) {
        if (ids.size() > MAX) {
            throw new OversizedBatchException(MAX, ids.size());
        }
        return ids;
    }
}
