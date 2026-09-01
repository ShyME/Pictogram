package me.imshy.pictogram.post.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class CaptionTooLongException extends ApiException {

    static final ProblemType TYPE = new ProblemType("post-caption-too-long", "Caption is too long");

    public CaptionTooLongException() {
        super(HttpStatus.BAD_REQUEST, TYPE, "A caption may be at most " + Caption.MAX_LENGTH + " characters.");
    }
}
