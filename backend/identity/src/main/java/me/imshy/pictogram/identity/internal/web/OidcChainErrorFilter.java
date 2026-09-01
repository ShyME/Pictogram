package me.imshy.pictogram.identity.internal.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import me.imshy.pictogram.shared.http.ProblemDetails;
import me.imshy.pictogram.shared.http.ProblemType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

class OidcChainErrorFilter extends OncePerRequestFilter {

    private static final Log log = LogFactory.getLog(OidcChainErrorFilter.class);

    private final ObjectMapper objectMapper;
    private final SignInCompletion completion;

    OidcChainErrorFilter(ObjectMapper objectMapper, SignInCompletion completion) {
        this.objectMapper = objectMapper;
        this.completion = completion;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (UnusableGoogleAccountException unusable) {
            if (response.isCommitted()) {
                throw unusable;
            }
            response.reset();
            completion.unusable(request, response, unusable.reason());
        } catch (RuntimeException | ServletException | IOException failure) {
            if (response.isCommitted()) {
                throw failure;
            }
            log.error("Unhandled failure in the Google sign-in filter chain", failure);
            completion.endHandshakeSession(request);
            response.reset();
            var problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Sign-in could not be completed. The failure has been logged.");
            problem.setType(ProblemType.INTERNAL_ERROR.uri());
            problem.setTitle(ProblemType.INTERNAL_ERROR.title());
            ProblemDetails.write(response, objectMapper, problem);
        }
    }
}
