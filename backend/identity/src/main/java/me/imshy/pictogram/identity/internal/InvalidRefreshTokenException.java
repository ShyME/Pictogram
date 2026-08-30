package me.imshy.pictogram.identity.internal;

/** The presented string is not a currently-usable refresh token (unknown or expired). */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
