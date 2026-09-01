package me.imshy.pictogram.shared.http;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes a {@link ProblemDetail} to a raw servlet response as {@code application/problem+json}
 * — for the security-filter callbacks and chain-level error handlers that fire before Spring
 * MVC's message converters are in play.
 */
public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static void write(HttpServletResponse response, ObjectMapper objectMapper, ProblemDetail problem)
            throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
