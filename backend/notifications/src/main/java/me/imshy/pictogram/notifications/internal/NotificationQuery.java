package me.imshy.pictogram.notifications.internal;

import java.util.List;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.Cursor;
import me.imshy.pictogram.shared.http.KeysetWindow;
import me.imshy.pictogram.shared.http.Limits;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

/**
 * The read side of {@code notifications} (#198). Public only so the sibling
 * {@code internal.web} controller can use it and {@link NotificationView}, as
 * {@code social}'s feed does.
 */
@Service
public class NotificationQuery {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final NotificationStore store;

    NotificationQuery(NotificationStore store) {
        this.store = store;
    }

    public Page pageFor(ViewerId recipient, Cursor after, Integer limit) {
        int pageSize = Limits.clamp(limit, DEFAULT_LIMIT, MAX_LIMIT);

        Limit fetch = Limit.of(pageSize + 1);
        List<Notification> rows = after == null
            ? store.newestFor(recipient.value(), fetch)
            : store.beforeFor(recipient.value(), after.at(), after.id(), fetch);

        KeysetWindow<Notification> window = KeysetWindow.of(rows, pageSize,
            last -> new Cursor(last.createdAt(), last.getId()));

        return new Page(window.page().stream().map(NotificationView::of).toList(), window.nextCursor());
    }

    public long unreadCountFor(ViewerId recipient) {
        return store.countByRecipientIdAndReadIsFalse(recipient.value());
    }

    public void markAllReadFor(ViewerId recipient) {
        store.markAllReadFor(recipient.value());
    }

    public record Page(List<NotificationView> notifications, Cursor nextCursor) {
    }
}
