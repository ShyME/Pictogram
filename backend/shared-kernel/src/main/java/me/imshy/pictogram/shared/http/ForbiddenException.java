package me.imshy.pictogram.shared.http;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String detail) {
        super(HttpStatus.FORBIDDEN, ProblemType.FORBIDDEN, detail);
    }
}
