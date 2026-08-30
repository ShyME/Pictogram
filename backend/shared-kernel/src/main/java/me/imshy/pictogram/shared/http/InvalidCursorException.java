package me.imshy.pictogram.shared.http;

import org.springframework.http.HttpStatus;

/** A pagination cursor that is not a well-formed token this API produces. */
public class InvalidCursorException extends ApiException {

    public InvalidCursorException() {
        super(HttpStatus.BAD_REQUEST, ProblemType.INVALID_CURSOR, "The pagination cursor is malformed.");
    }
}
