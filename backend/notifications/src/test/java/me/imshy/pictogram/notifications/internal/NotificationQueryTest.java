package me.imshy.pictogram.notifications.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.imshy.pictogram.notifications.internal.NotificationQuery.Page;
import me.imshy.pictogram.shared.ViewerId;
import me.imshy.pictogram.shared.http.Cursor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The read side of {@code notifications} (#198): keyset paging newest-first
 * with the feed's cursor contract, an unread count scoped to the caller, and a
 * bulk mark-read that is idempotent. One recipient never sees another's rows.
 */
class NotificationQueryTest extends NotificationsModuleIntegrationTest {

    @Autowired
    private NotificationQuery query;

    @Test
    void anEmptyInboxIsAnEmptyPageWithNoCursor() {
        Page page = query.pageFor(viewer(UUID.randomUUID()), null, null);

        assertThat(page.notifications()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void theFirstPageIsNewestFirstAndCarriesACursorWhileMoreRemain() {
        UUID recipient = UUID.randomUUID();
        List<UUID> oldestFirst = seed(recipient, 5);

        Page page = query.pageFor(viewer(recipient), null, 2);

        assertThat(actorIds(page)).containsExactly(oldestFirst.get(4), oldestFirst.get(3));
        assertThat(page.nextCursor()).isNotNull();
    }

    @Test
    void theDefaultPageSizeIsTwenty() {
        UUID recipient = UUID.randomUUID();
        seed(recipient, 21);

        Page page = query.pageFor(viewer(recipient), null, null);

        assertThat(page.notifications()).hasSize(20);
        assertThat(page.nextCursor()).isNotNull();
    }

    @Test
    void aMidCursorContinuesWithoutGapsOrDuplicates() {
        UUID recipient = UUID.randomUUID();
        List<UUID> oldestFirst = seed(recipient, 5);

        List<UUID> walked = new ArrayList<>();
        Cursor cursor = null;
        do {
            Page page = query.pageFor(viewer(recipient), cursor, 2);
            walked.addAll(actorIds(page));
            cursor = page.nextCursor();
        } while (cursor != null);

        assertThat(walked).containsExactly(oldestFirst.get(4), oldestFirst.get(3), oldestFirst.get(2),
            oldestFirst.get(1), oldestFirst.get(0));
    }

    @Test
    void theLastPageHasNoCursor() {
        UUID recipient = UUID.randomUUID();
        seed(recipient, 4);

        Page page = query.pageFor(viewer(recipient), null, 4);

        assertThat(page.notifications()).hasSize(4);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void theUnreadCountCountsOnlyTheCallersUnread() {
        UUID caller = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        seed(caller, 2);
        query.markAllReadFor(viewer(caller));
        seed(caller, 3);
        seed(other, 4);

        assertThat(query.unreadCountFor(viewer(caller))).isEqualTo(3);
        assertThat(query.unreadCountFor(viewer(other))).isEqualTo(4);
    }

    @Test
    void markReadFlipsEveryUnreadOfTheCallerAndIsIdempotent() {
        UUID caller = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        seed(caller, 3);
        seed(other, 2);

        query.markAllReadFor(viewer(caller));
        assertThat(query.unreadCountFor(viewer(caller))).isZero();

        query.markAllReadFor(viewer(caller));
        assertThat(query.unreadCountFor(viewer(caller))).isZero();

        assertThat(query.pageFor(viewer(caller), null, 50).notifications()).hasSize(3).allMatch(NotificationView::read);
        assertThat(query.unreadCountFor(viewer(other))).isEqualTo(2);
    }

    @Test
    void aRecipientOnlyEverSeesTheirOwnNotifications() {
        UUID caller = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        List<UUID> callerActors = seed(caller, 2);
        seed(other, 3);

        Page page = query.pageFor(viewer(caller), null, 50);

        assertThat(actorIds(page)).containsExactlyInAnyOrderElementsOf(callerActors);
    }

    /**
     * Seed {@code count} notifications for one recipient, one second apart; returns
     * the actor ids oldest-first.
     */
    private List<UUID> seed(UUID recipient, int count) {
        List<UUID> actors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID actor = UUID.randomUUID();
            givenNotification(NotificationType.POST_LIKED, recipient, actor, UUID.randomUUID(),
                OCCURRED_AT.plusSeconds(i), OCCURRED_AT.plusSeconds(i));
            actors.add(actor);
        }
        return actors;
    }

    private static List<UUID> actorIds(Page page) {
        return page.notifications().stream().map(view -> view.actorId().value()).toList();
    }

    private static ViewerId viewer(UUID id) {
        return new ViewerId(id);
    }
}
