package me.imshy.pictogram.profile.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

/** The chosen username is well-formed but already belongs to another profile. */
public final class UsernameAlreadyTakenException extends ApiException {

    static final ProblemType TYPE = new ProblemType("username-taken", "That username is taken");

    public UsernameAlreadyTakenException() {
        super(HttpStatus.CONFLICT, TYPE, "That username is already taken. Choose another.");
    }
}
