package me.imshy.pictogram.profile.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class AlreadyOnboardedException extends ApiException {

    static final ProblemType TYPE = new ProblemType("already-onboarded", "You already have a profile");

    public AlreadyOnboardedException() {
        super(HttpStatus.CONFLICT, TYPE, "This account has already completed onboarding.");
    }
}
