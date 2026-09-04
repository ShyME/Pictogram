package me.imshy.pictogram.social.internal.comment;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class EmptyCommentException extends ApiException {

    static final ProblemType TYPE = new ProblemType("comment-empty", "Comment is empty");

    public EmptyCommentException() {
        super(HttpStatus.BAD_REQUEST, TYPE, "A comment can't be empty.");
    }
}
