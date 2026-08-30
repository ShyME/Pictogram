package me.imshy.pictogram.shared.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders exceptions thrown from a controller as RFC 9457 Problem Details. Expected
 * failures come through {@link ApiException} and carry a stable {@link ProblemType};
 * Spring MVC's own exceptions keep their built-in problem bodies ({@code about:blank},
 * which RFC 9457 §4.1 permits); anything else is a 500 whose body never leaks a stack
 * trace.
 */
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Log log = LogFactory.getLog(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        return problem(ex.toProblemDetail());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        return problem(new UnauthenticatedException(ex.getMessage()).toProblemDetail());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(new ForbiddenException(ex.getMessage()).toProblemDetail());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception serving an API request", ex);
        var body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "The server could not process the request. The failure has been logged.");
        body.setType(ProblemType.INTERNAL_ERROR.uri());
        body.setTitle(ProblemType.INTERNAL_ERROR.title());
        return problem(body);
    }

    private static ResponseEntity<ProblemDetail> problem(ProblemDetail body) {
        return ResponseEntity.status(body.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
