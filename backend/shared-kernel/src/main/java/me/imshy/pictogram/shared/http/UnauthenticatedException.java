package me.imshy.pictogram.shared.http;

import org.springframework.http.HttpStatus;

/** No usable Pictogram access token on a request that needs the current user. */
public class UnauthenticatedException extends ApiException {

    public UnauthenticatedException(String detail) {
        super(HttpStatus.UNAUTHORIZED, ProblemType.UNAUTHORIZED, detail);
    }
}
