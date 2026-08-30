package me.imshy.pictogram.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ViewerIdTest {

    @Test
    void roundTripsThroughItsStringForm() {
        var viewer = ViewerId.random();

        assertThat(ViewerId.fromString(viewer.toString())).isEqualTo(viewer);
    }

    @Test
    void rejectsANullValue() {
        assertThatNullPointerException().isThrownBy(() -> new ViewerId(null));
    }

    @Test
    void isNeverEqualToTheUserIdItWraps() {
        var uuid = UUID.randomUUID();

        assertThat((Object) new ViewerId(uuid)).isNotEqualTo(new UserId(uuid));
    }

    @Test
    void convertsToAndFromTheUnderlyingUserId() {
        var user = UserId.random();

        assertThat(ViewerId.of(user).asUserId()).isEqualTo(user);
    }
}
