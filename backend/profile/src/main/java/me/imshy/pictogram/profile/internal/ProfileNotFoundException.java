package me.imshy.pictogram.profile.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class ProfileNotFoundException extends ApiException {

    static final ProblemType TYPE = new ProblemType("profile-not-found", "Profile not found");

    public ProfileNotFoundException() {
        super(HttpStatus.NOT_FOUND, TYPE, "This user has no profile yet.");
    }
}
