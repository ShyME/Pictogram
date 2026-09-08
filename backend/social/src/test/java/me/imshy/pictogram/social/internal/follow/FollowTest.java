package me.imshy.pictogram.social.internal.follow;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Instant;
import me.imshy.pictogram.shared.UserId;
import org.junit.jupiter.api.Test;

class FollowTest {

    @Test
    void aFollowCannotBeReflexive() {
        var self = UserId.random();

        assertThatExceptionOfType(SelfFollowException.class).isThrownBy(() -> Follow.of(self, self, Instant.now()));
    }

    @Test
    void aFollowBetweenTwoDistinctUsersIsAllowed() {
        assertThatNoException().isThrownBy(() -> Follow.of(UserId.random(), UserId.random(), Instant.now()));
    }
}
