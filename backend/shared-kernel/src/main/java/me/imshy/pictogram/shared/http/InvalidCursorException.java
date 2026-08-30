package me.imshy.pictogram.shared.http;

import org.springframework.http.HttpStatus;

/** A pagination cursor that is malformed or was not issued by the endpoint it was sent to. */
public class InvalidCursorException extends ApiException {

    public InvalidCursorException() {
        super(HttpStatus.BAD_REQUEST, "invalid-cursor", "Invalid pagination cursor",
                "The pagination cursor is malformed or was not issued by this endpoint.");
    }
}
