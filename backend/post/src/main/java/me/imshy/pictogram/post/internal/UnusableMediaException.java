package me.imshy.pictogram.post.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class UnusableMediaException extends ApiException {

    static final ProblemType TYPE = new ProblemType("post-media-unusable", "That image can't be used");

    public UnusableMediaException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, TYPE,
                "That image can't be used for a post. Upload the photo again and retry.");
    }
}
