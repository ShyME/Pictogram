package me.imshy.pictogram.shared.http;

import org.springframework.http.HttpStatus;

public final class RateLimitExceededException extends ApiException {

    static final ProblemType TYPE = new ProblemType("rate-limit-exceeded", "Too many requests");

    public RateLimitExceededException(String action) {
        super(
                HttpStatus.TOO_MANY_REQUESTS,
                TYPE,
                "Too many \"%s\" requests. Slow down and try again shortly.".formatted(action));
    }
}
