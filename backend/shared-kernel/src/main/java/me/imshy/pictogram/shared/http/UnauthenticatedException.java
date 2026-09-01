package me.imshy.pictogram.shared.http;

import org.springframework.http.HttpStatus;

public class UnauthenticatedException extends ApiException {

    public UnauthenticatedException(String detail) {
        super(HttpStatus.UNAUTHORIZED, ProblemType.UNAUTHORIZED, detail);
    }
}
