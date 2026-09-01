package me.imshy.pictogram.post.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

/**
 * The {@code MediaId} a publish request carries cannot back a post: either no media has
 * that id, or it was uploaded by someone other than the author. The two are one failure on
 * the wire — the response does not say which, so it reveals nothing about which media ids
 * exist.
 */
public final class UnusableMediaException extends ApiException {

    static final ProblemType TYPE = new ProblemType("post-media-unusable", "That image can't be used");

    public UnusableMediaException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, TYPE,
                "That image can't be used for a post. Upload the photo again and retry.");
    }
}
