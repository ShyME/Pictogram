import { api, fetchAccounts, fetchPosts, throwIfSessionExpired } from '@shared';
import { type NotificationCard, toNotification } from './notification';

export type NotificationsPage = { cards: NotificationCard[]; nextCursor: string | null };

// One page of notifications plus the two batched lookups it needs — the actors and the
// posts the like / comment rows point at — so a row never fans out a request (ADR-0005).
export async function fetchNotificationsPage(cursor?: string): Promise<NotificationsPage> {
  const { data, response } = await api.GET('/api/notifications', {
    params: { query: { cursor } },
  });
  throwIfSessionExpired(response);
  if (!data) throw new Error(`Notifications request failed: ${response.status}`);

  const notifications = (data.items ?? []).map((item) => toNotification(item));
  const actorIds = [...new Set(notifications.map((notification) => notification.actorId))];
  const postIds = [
    ...new Set(
      notifications
        .map((notification) => notification.subjectPostId)
        .filter((id): id is string => id !== null),
    ),
  ];

  const [actors, posts] = await Promise.all([fetchAccounts(actorIds), fetchPosts(postIds)]);

  return {
    cards: notifications.map((notification) => ({
      ...notification,
      actor: actors.get(notification.actorId) ?? null,
      postMediaId: notification.subjectPostId
        ? (posts.get(notification.subjectPostId)?.mediaId ?? null)
        : null,
    })),
    nextCursor: data.nextCursor ?? null,
  };
}

export async function fetchUnreadCount(): Promise<number> {
  const { data, response } = await api.GET('/api/notifications/unread-count');
  throwIfSessionExpired(response);
  if (!data) throw new Error(`Unread-count request failed: ${response.status}`);
  return data.count ?? 0;
}

export async function markAllNotificationsRead(): Promise<void> {
  const { response } = await api.POST('/api/notifications/mark-read');
  throwIfSessionExpired(response);
  if (!response.ok) throw new Error(`Marking notifications read failed: ${response.status}`);
}
