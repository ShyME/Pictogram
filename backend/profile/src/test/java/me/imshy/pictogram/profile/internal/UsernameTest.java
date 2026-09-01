package me.imshy.pictogram.profile.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class UsernameTest {

    @ParameterizedTest
    @ValueSource(strings = {"ada", "ada_lovelace", "a_1", "user_name_12345678", "exactly_twenty_chars", "___"})
    void acceptsAHandleOfThreeToTwentyLowercaseWordCharacters(String candidate) {
        assertThat(new Username(candidate).value()).isEqualTo(candidate);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "ad",
            "twenty_one_characters",
            "Ada",
            "ada lovelace",
            "ada-lovelace",
            "ada.lovelace",
            "adaünicode",
            "",
            "   "})
    void rejectsAHandleThatDoesNotMatchTheShapeRule(String candidate) {
        assertThatExceptionOfType(MalformedUsernameException.class)
                .isThrownBy(() -> new Username(candidate));
    }

    @Test
    void theShapeRejectionIsA400ProblemDetailDistinctFromTaken() {
        var shape = new MalformedUsernameException();
        var taken = new UsernameAlreadyTakenException();

        assertThat(shape.toProblemDetail().getStatus()).isEqualTo(400);
        assertThat(shape.toProblemDetail().getType()).isNotEqualTo(taken.toProblemDetail().getType());
    }
}
