package me.imshy.pictogram.post.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/** The caption rule: optional, trimmed, at most 2200 code points; blank means "no caption". */
class CaptionTest {

    @Test
    void aBlankOrNullCaptionIsCarriedAsNull() {
        assertThat(Caption.of(null).value()).isNull();
        assertThat(Caption.of("   ").value()).isNull();
        assertThat(Caption.of("").value()).isNull();
    }

    @Test
    void aCaptionIsTrimmedOfSurroundingWhitespace() {
        assertThat(Caption.of("  a first post  ").value()).isEqualTo("a first post");
    }

    @Test
    void aCaptionAtTheLimitIsAccepted() {
        String atLimit = "x".repeat(Caption.MAX_LENGTH);

        assertThat(Caption.of(atLimit).value()).isEqualTo(atLimit);
    }

    @Test
    void aCaptionOverTheLimitIsRejected() {
        String tooLong = "x".repeat(Caption.MAX_LENGTH + 1);

        assertThatExceptionOfType(CaptionTooLongException.class).isThrownBy(() -> Caption.of(tooLong));
    }

    @Test
    void lengthIsCountedInCodePointsSoAnEmojiCostsOne() {
        String emoji = "📷".repeat(Caption.MAX_LENGTH); // camera emoji, 2 chars / 1 code point

        assertThat(Caption.of(emoji).value()).isEqualTo(emoji);
    }
}
