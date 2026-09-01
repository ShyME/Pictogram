package me.imshy.pictogram.post.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class PostNotFoundException extends ApiException {

    static final ProblemType TYPE = new ProblemType("post-not-found", "No such post");

    public PostNotFoundException() {
        super(HttpStatus.NOT_FOUND, TYPE, "That post doesn't exist.");
    }
}
