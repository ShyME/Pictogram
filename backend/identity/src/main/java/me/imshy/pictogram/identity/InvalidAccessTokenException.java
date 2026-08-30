package me.imshy.pictogram.identity;

/** The presented string is not a currently-valid Pictogram access token. */
public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
