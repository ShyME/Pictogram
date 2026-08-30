package me.imshy.pictogram.shared.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

/**
 * Renders the 401 for a request the security filter chain rejects before it reaches a
 * controller — a missing or invalid access token — as the same Problem Detail an
 * {@link UnauthenticatedException} thrown from a controller would produce.
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        var problem = new UnauthenticatedException("A valid Pictogram access token is required.")
                .toProblemDetail();
        ProblemDetails.write(response, objectMapper, problem);
    }
}
