package me.imshy.pictogram.shared.http;

import java.net.URI;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * An expected failure that renders as an RFC 9457 Problem Detail. Modules throw a subclass
 * (or this directly) from their web layer; {@link ApiExceptionHandler} turns it into an
 * {@code application/problem+json} response. Every instance carries a stable {@code type}
 * URI so clients can branch on the failure without parsing prose.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final URI type;
    private final String title;

    public ApiException(HttpStatus status, String typeSlug, String title, String detail) {
        super(detail);
        this.status = Objects.requireNonNull(status, "status");
        this.type = ProblemType.of(Objects.requireNonNull(typeSlug, "typeSlug"));
        this.title = Objects.requireNonNull(title, "title");
    }

    public HttpStatusCode getStatusCode() {
        return status;
    }

    public ProblemDetail toProblemDetail() {
        var problem = ProblemDetail.forStatusAndDetail(status, getMessage());
        problem.setType(type);
        problem.setTitle(title);
        return problem;
    }
}
