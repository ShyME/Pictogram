package me.imshy.pictogram.shared.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CursorTest {

    @Test
    void encodeThenDecodeRoundTrips() {
        var cursor = new Cursor(Instant.parse("2026-08-30T12:34:56Z"), UUID.randomUUID());

        assertThat(Cursor.decode(cursor.encode())).isEqualTo(cursor);
    }

    @Test
    void preservesSubSecondPrecisionAcrossTheRoundTrip() {
        var cursor = new Cursor(Instant.ofEpochSecond(1_700_000_000L, 123_456_789), UUID.randomUUID());

        assertThat(Cursor.decode(cursor.encode())).isEqualTo(cursor);
    }

    @Test
    void encodesToAnOpaqueTokenThatLeaksNeitherComponent() {
        var id = UUID.randomUUID();
        var cursor = new Cursor(Instant.parse("2026-08-30T12:34:56Z"), id);

        var encoded = cursor.encode();

        assertThat(encoded).doesNotContain(id.toString()).doesNotContain("2026-08-30");
    }

    @Test
    void rejectsATokenThatIsNotBase64() {
        assertThatExceptionOfType(InvalidCursorException.class)
                .isThrownBy(() -> Cursor.decode("not base64 !!!"));
    }

    @Test
    void rejectsABase64TokenWithoutTheComponentSeparator() {
        var noSeparator = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("2026-08-30T12:34:56Z".getBytes());

        assertThatExceptionOfType(InvalidCursorException.class)
                .isThrownBy(() -> Cursor.decode(noSeparator));
    }

    @Test
    void rejectsABase64TokenWhoseIdIsNotAUuid() {
        var badId = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("2026-08-30T12:34:56Z|not-a-uuid".getBytes());

        assertThatExceptionOfType(InvalidCursorException.class)
                .isThrownBy(() -> Cursor.decode(badId));
    }

    @Test
    void rejectsAnEmptyToken() {
        assertThatExceptionOfType(InvalidCursorException.class)
                .isThrownBy(() -> Cursor.decode(""));
    }
}
