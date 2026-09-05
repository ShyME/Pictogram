package me.imshy.pictogram.shared.http;

public final class Limits {

    private Limits() {
    }

    public static int clamp(Integer requested, int defaultLimit, int maxLimit) {
        return requested == null ? defaultLimit : Math.clamp(requested, 1, maxLimit);
    }
}
