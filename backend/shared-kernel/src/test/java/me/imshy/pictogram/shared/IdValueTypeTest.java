package me.imshy.pictogram.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdValueTypeTest {

    @Test
    void roundTripsThroughItsStringForm() {
        var id = UserId.random();

        assertThat(UserId.fromString(id.toString())).isEqualTo(id);
    }

    @Test
    void rejectsANullValue() {
        assertThatNullPointerException().isThrownBy(() -> new PostId(null));
    }

    @Test
    void distinctTypesAreNeverEqualEvenWithTheSameUuid() {
        var uuid = UUID.randomUUID();

        assertThat((Object) new UserId(uuid)).isNotEqualTo(new MediaId(uuid));
    }
}
