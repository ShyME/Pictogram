package me.imshy.pictogram.shared.http;

/** The page-limit clamp idiom shared by keyset endpoints: default when unset, otherwise bounded to [1, max]. */
public final class Limits {

    private Limits() {}

    public static int clamp(Integer requested, int defaultLimit, int maxLimit) {
        return requested == null ? defaultLimit : Math.clamp(requested, 1, maxLimit);
    }
}
