package me.imshy.pictogram.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class IdJsonTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void userIdSerialisesAsAPlainUuidString() {
        var id = UserId.random();

        assertThat(json.writeValueAsString(id)).isEqualTo("\"" + id + "\"");
    }

    @Test
    void userIdRoundTripsThroughJson() {
        var id = UserId.random();

        assertThat(json.readValue(json.writeValueAsString(id), UserId.class)).isEqualTo(id);
    }

    @Test
    void viewerIdRoundTripsThroughJson() {
        var id = ViewerId.random();

        assertThat(json.readValue(json.writeValueAsString(id), ViewerId.class)).isEqualTo(id);
    }

    @Test
    void postIdAndMediaIdRoundTripThroughJson() {
        var post = PostId.random();
        var media = MediaId.random();

        assertThat(json.readValue(json.writeValueAsString(post), PostId.class)).isEqualTo(post);
        assertThat(json.readValue(json.writeValueAsString(media), MediaId.class))
                .isEqualTo(media);
    }
}
