package me.imshy.pictogram.profile.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class InvalidProfileDetailsException extends ApiException {

    static final ProblemType TYPE = new ProblemType("profile-details-invalid", "Profile details are invalid");

    public InvalidProfileDetailsException(String detail) {
        super(HttpStatus.BAD_REQUEST, TYPE, detail);
    }
}
