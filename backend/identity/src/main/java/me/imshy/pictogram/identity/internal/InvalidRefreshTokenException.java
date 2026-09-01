package me.imshy.pictogram.identity.internal;

/**
 * The presented string is not a currently-usable refresh token — unknown, expired, or a
 * just-consumed token replayed within the rotation grace (a benign concurrent refresh, which
 * unlike {@link RefreshTokenReuseException} leaves the family intact).
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
