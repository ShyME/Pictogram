package me.imshy.pictogram.shared.http;

import org.springframework.http.HttpStatus;

/** The current user is authenticated but not allowed to perform this action. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String detail) {
        super(HttpStatus.FORBIDDEN, ProblemType.FORBIDDEN, detail);
    }
}
