package me.imshy.pictogram.profile.internal;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

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
