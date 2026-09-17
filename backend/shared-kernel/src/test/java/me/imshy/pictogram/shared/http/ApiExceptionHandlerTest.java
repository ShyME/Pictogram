package me.imshy.pictogram.shared.http;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Sentry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @AfterEach
    void closeSentry() {
        Sentry.close();
    }

    @Test
    void capturesAnUnhandledExceptionInSentryBeforeRespondingWithProblemJson() {
        List<Throwable> captured = new ArrayList<>();
        Sentry.init(options -> {
            options.setDsn("https://public@sentry.example.com/1");
            // Records the throwable and drops the event — no network call leaves this test.
            options.setBeforeSend((event, hint) -> {
                captured.add(event.getThrowable());
                return null;
            });
        });
        var exception = new IllegalStateException("boom");

        var response = handler.handleUnexpected(exception);

        assertThat(captured).containsExactly(exception);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void stillRespondsWithProblemJsonWhenSentryIsNotConfigured() {
        var response = handler.handleUnexpected(new IllegalStateException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail())
                .isEqualTo("The server could not process the request. The failure has been logged.");
    }
}
