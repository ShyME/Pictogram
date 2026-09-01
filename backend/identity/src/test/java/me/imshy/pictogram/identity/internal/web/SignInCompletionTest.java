package me.imshy.pictogram.identity.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import me.imshy.pictogram.identity.internal.AuthProperties;
import me.imshy.pictogram.identity.internal.Session;
import me.imshy.pictogram.identity.internal.web.UnusableGoogleAccountException.Reason;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

/**
 * Exercises the handshake-completion decision on its own — no {@code mock-oauth2-server}, no
 * servlet chain. The invariants under test: the refresh cookie is written on success and
 * only on success, each outcome lands on the right redirect, and every terminal path ends
 * the handshake servlet session exactly once (and does not NPE when there is none) (#27).
 */
class SignInCompletionTest {

    private static final AuthProperties PROPERTIES = new AuthProperties(
            Duration.ofMinutes(15), Duration.ofDays(30), Duration.ofSeconds(60), "pictogram", null,
            "/home", "/login?error=sign-in-failed", false);

    private final SignInCompletion completion = new SignInCompletion(PROPERTIES);

    @FunctionalInterface
    private interface Outcome {
        void complete(SignInCompletion completion, HttpServletRequest request, HttpServletResponse response)
                throws Exception;
    }

    private static final Outcome SUCCEEDED =
            (c, req, res) -> c.succeeded(req, res, session("refresh-token-abc"));
    private static final Outcome UNUSABLE_UNVERIFIED =
            (c, req, res) -> c.unusable(req, res, Reason.EMAIL_UNVERIFIED);
    private static final Outcome UNUSABLE_MISSING =
            (c, req, res) -> c.unusable(req, res, Reason.EMAIL_MISSING);
    private static final Outcome FAILED = SignInCompletion::failed;

    @Test
    void successWritesTheRefreshCookieAndRedirectsToThePostLoginTarget() throws Exception {
        var response = new MockHttpServletResponse();

        completion.succeeded(new MockHttpServletRequest(), response, session("refresh-token-abc"));

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header)
                        .startsWith("pictogram_refresh=refresh-token-abc")
                        .contains("Max-Age=" + Duration.ofDays(30).toSeconds())
                        .contains("Path=/api/auth"));
        assertThat(response.getRedirectedUrl()).isEqualTo("/home");
    }

    @Test
    void unusableRedirectsToTheReasonSlugAndWritesNoCookie() throws Exception {
        var response = new MockHttpServletResponse();

        completion.unusable(new MockHttpServletRequest(), response, Reason.EMAIL_UNVERIFIED);

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=email-unverified");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    void unusableCarriesTheEmailMissingSlug() throws Exception {
        var response = new MockHttpServletResponse();

        completion.unusable(new MockHttpServletRequest(), response, Reason.EMAIL_MISSING);

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=email-missing");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    void failedRedirectsToTheGenericSignInErrorRouteAndWritesNoCookie() throws Exception {
        var response = new MockHttpServletResponse();

        completion.failed(new MockHttpServletRequest(), response);

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=sign-in-failed");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    void onlySuccessWritesACookie() throws Exception {
        assertThat(cookieHeadersAfter(UNUSABLE_UNVERIFIED)).isEmpty();
        assertThat(cookieHeadersAfter(FAILED)).isEmpty();
        assertThat(cookieHeadersAfter(SUCCEEDED)).isNotEmpty();
    }

    @Test
    void everyOutcomeEndsTheHandshakeSessionExactlyOnce() throws Exception {
        for (Outcome outcome : new Outcome[] {SUCCEEDED, UNUSABLE_UNVERIFIED, UNUSABLE_MISSING, FAILED}) {
            HttpSession handshakeSession = spy(new MockHttpSession());
            var request = new MockHttpServletRequest();
            request.setSession(handshakeSession);

            outcome.complete(completion, request, new MockHttpServletResponse());

            verify(handshakeSession, times(1)).invalidate();
        }
    }

    @Test
    void endingTheHandshakeToleratesTheAbsenceOfAServletSession() throws Exception {
        for (Outcome outcome : new Outcome[] {SUCCEEDED, UNUSABLE_MISSING, FAILED}) {
            var response = new MockHttpServletResponse();

            outcome.complete(completion, new MockHttpServletRequest(), response);

            assertThat(response.getRedirectedUrl()).isNotNull();
        }
    }

    private List<String> cookieHeadersAfter(Outcome outcome) throws Exception {
        var response = new MockHttpServletResponse();
        outcome.complete(completion, new MockHttpServletRequest(), response);
        return response.getHeaders(HttpHeaders.SET_COOKIE);
    }

    private static Session session(String refreshToken) {
        return new Session("access-token", Instant.now().plusSeconds(900),
                refreshToken, Instant.now().plusSeconds(3600));
    }
}
