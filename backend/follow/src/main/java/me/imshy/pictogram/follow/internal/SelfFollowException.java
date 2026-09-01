package me.imshy.pictogram.follow.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class SelfFollowException extends ApiException {

    static final ProblemType TYPE = new ProblemType("self-follow", "You can't follow yourself");

    public SelfFollowException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, TYPE, "You can't follow yourself.");
    }
}
