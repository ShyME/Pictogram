package me.imshy.pictogram.profile.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

/** No profile exists for the requested user — most often a user who has not onboarded yet. */
public final class ProfileNotFoundException extends ApiException {

    static final ProblemType TYPE = new ProblemType("profile-not-found", "Profile not found");

    public ProfileNotFoundException() {
        super(HttpStatus.NOT_FOUND, TYPE, "This user has no profile yet.");
    }
}
