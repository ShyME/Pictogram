package me.imshy.pictogram.shared.http;

import java.net.URI;

/**
 * Builds the stable {@code type} URI for an RFC 9457 Problem Detail. The URIs are stable
 * identifiers a client can branch on; they are not meant to be dereferenced.
 */
public final class ProblemType {

    public static final String BASE = "https://pictogram.dev/problems/";

    private ProblemType() {
    }

    public static URI of(String slug) {
        return URI.create(BASE + slug);
    }
}
