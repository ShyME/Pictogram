package me.imshy.pictogram.media.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

/** No media exists for the requested {@code MediaId}. */
public final class MediaNotFoundException extends ApiException {

    static final ProblemType TYPE = new ProblemType("media-not-found", "Media not found");

    public MediaNotFoundException() {
        super(HttpStatus.NOT_FOUND, TYPE, "No media exists with that id.");
    }
}
