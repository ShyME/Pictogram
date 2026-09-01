package me.imshy.pictogram.shared.http;

import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

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
