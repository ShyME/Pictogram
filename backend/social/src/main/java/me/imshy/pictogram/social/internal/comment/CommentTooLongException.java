package me.imshy.pictogram.social.internal.comment;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class CommentTooLongException extends ApiException {

    static final ProblemType TYPE = new ProblemType("comment-too-long", "Comment is too long");

    public CommentTooLongException() {
        super(HttpStatus.BAD_REQUEST, TYPE, "A comment may be at most " + CommentBody.MAX_LENGTH + " characters.");
    }
}
