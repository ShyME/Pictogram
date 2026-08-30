package me.imshy.pictogram.shared.http;

import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * An expected failure that renders as an RFC 9457 Problem Detail. Modules throw a subclass
 * (or this directly) from their web layer; {@link ApiExceptionHandler} turns it into an
 * {@code application/problem+json} response carrying the {@link ProblemType}'s stable
 * {@code type} URI so clients can branch on the failure without parsing prose.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ProblemType type;

    public ApiException(HttpStatus status, ProblemType type, String detail) {
        super(detail);
        this.status = Objects.requireNonNull(status, "status");
        this.type = Objects.requireNonNull(type, "type");
    }

    public HttpStatusCode getStatusCode() {
        return status;
    }

    public ProblemDetail toProblemDetail() {
        var problem = ProblemDetail.forStatusAndDetail(status, getMessage());
        problem.setType(type.uri());
        problem.setTitle(type.title());
        return problem;
    }
}
