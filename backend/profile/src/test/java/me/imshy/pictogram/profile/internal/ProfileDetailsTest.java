package me.imshy.pictogram.profile.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

/** The optional display-name and bio value objects: blank handling, length in code points. */
class ProfileDetailsTest {

    @Test
    void blankOrNullInputBecomesNotSet() {
        assertThat(DisplayName.of(null).value()).isNull();
        assertThat(DisplayName.of("   ").value()).isNull();
        assertThat(Bio.of("").value()).isNull();
    }

    @Test
    void displayNameLengthIsCountedInCodePointsSoAnEmojiCostsOne() {
        assertThatNoException().isThrownBy(() -> DisplayName.of("🎨".repeat(DisplayName.MAX_LENGTH)));
        assertThatExceptionOfType(InvalidProfileDetailsException.class)
                .isThrownBy(() -> DisplayName.of("🎨".repeat(DisplayName.MAX_LENGTH + 1)));
    }

    @Test
    void displayNameRejectsLineBreaksAndControlCharacters() {
        assertThatExceptionOfType(InvalidProfileDetailsException.class)
                .isThrownBy(() -> DisplayName.of("Ada\nLovelace"));
        assertThatExceptionOfType(InvalidProfileDetailsException.class)
                .isThrownBy(() -> DisplayName.of("Ada\tLovelace"));
    }

    @Test
    void bioLengthIsCountedInCodePointsAndMaySpanLines() {
        assertThatNoException().isThrownBy(() -> Bio.of("🎨".repeat(Bio.MAX_LENGTH)));
        assertThatNoException().isThrownBy(() -> Bio.of("line one\nline two"));
        assertThatExceptionOfType(InvalidProfileDetailsException.class)
                .isThrownBy(() -> Bio.of("x".repeat(Bio.MAX_LENGTH + 1)));
    }

    @Test
    void surroundingWhitespaceIsTrimmed() {
        assertThat(DisplayName.of("  Ada Lovelace  ").value()).isEqualTo("Ada Lovelace");
    }
}
