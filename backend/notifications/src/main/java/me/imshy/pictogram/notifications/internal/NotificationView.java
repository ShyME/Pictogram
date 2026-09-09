package me.imshy.pictogram.notifications.internal;

import java.time.Instant;
import me.imshy.pictogram.shared.PostId;
import me.imshy.pictogram.shared.UserId;

public record NotificationView(String type, UserId actorId, PostId subjectPostId, Instant occurredAt, boolean read) {

    static NotificationView of(Notification notification) {
        return new NotificationView(notification.type().wireName(), notification.actorId(), notification.subjectId(),
            notification.occurredAt(), notification.read());
    }
}
