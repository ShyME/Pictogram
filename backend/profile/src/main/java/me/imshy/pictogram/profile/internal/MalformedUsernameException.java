package me.imshy.pictogram.profile.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class MalformedUsernameException extends ApiException {

    static final ProblemType TYPE = new ProblemType("username-invalid", "Username has the wrong shape");

    public MalformedUsernameException() {
        super(
                HttpStatus.BAD_REQUEST,
                TYPE,
                "A username must be 3 to 20 characters of lowercase letters, digits or underscore.");
    }
}
