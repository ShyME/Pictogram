package me.imshy.pictogram.notifications.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NotificationTypeTest {

    @Test
    void mapsEachAgreedWireNameToItsType() {
        assertThat(NotificationType.fromWire("post-liked")).isEqualTo(NotificationType.POST_LIKED);
        assertThat(NotificationType.fromWire("post-commented")).isEqualTo(NotificationType.POST_COMMENTED);
        assertThat(NotificationType.fromWire("user-followed")).isEqualTo(NotificationType.USER_FOLLOWED);
    }

    @Test
    void rejectsAnUnknownWireNameSoTheRecordIsDeadLettered() {
        assertThatThrownBy(() -> NotificationType.fromWire("post-unliked"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
