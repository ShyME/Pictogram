package me.imshy.pictogram.identity.internal.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import me.imshy.pictogram.identity.internal.AuthProperties;
import me.imshy.pictogram.shared.http.ProblemDetails;
import me.imshy.pictogram.shared.http.ProblemType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * The Google sign-in filter chain has no {@code DispatcherServlet} behind it, so the shared
 * {@code ApiExceptionHandler} never sees what it throws — an unhandled exception from
 * {@link OidcSignInSuccessHandler} or the OAuth2 filters would otherwise reach the container
 * as a white-label 500. This wraps the chain and renders anything unexpected as
 * {@code application/problem+json}, the same shape the API edge produces (#27).
 *
 * <p>{@link UnusableGoogleAccountException} is caught here only as a backstop — the success
 * handler already turns it into a redirect; if it ever escapes, the browser still gets the
 * SPA error route rather than a Problem Detail it can't act on.
 */
class OidcChainErrorFilter extends OncePerRequestFilter {

    private static final Log log = LogFactory.getLog(OidcChainErrorFilter.class);

    private final ObjectMapper objectMapper;
    private final AuthProperties properties;

    OidcChainErrorFilter(ObjectMapper objectMapper, AuthProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
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
            // The handshake may have persisted a SecurityContext to the session before failing.
            HandshakeSession.end(request);
            response.reset();
            response.sendRedirect(SignInErrorRedirect.withReason(
                    properties.signInErrorRedirect(), unusable.reason().slug()));
        } catch (RuntimeException | ServletException | IOException failure) {
            if (response.isCommitted()) {
                throw failure;
            }
            log.error("Unhandled failure in the Google sign-in filter chain", failure);
            HandshakeSession.end(request);
            response.reset();
            var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Sign-in could not be completed. The failure has been logged.");
            problem.setType(ProblemType.INTERNAL_ERROR.uri());
            problem.setTitle(ProblemType.INTERNAL_ERROR.title());
            ProblemDetails.write(response, objectMapper, problem);
        }
    }
}
