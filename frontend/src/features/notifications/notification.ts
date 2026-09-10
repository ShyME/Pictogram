import type { Account, components } from '@shared';

export type NotificationType = 'post-liked' | 'post-commented' | 'user-followed';

export type Notification = {
  type: NotificationType;
  actorId: string;
  subjectPostId: string | null;
  occurredAt: string;
  read: boolean;
};

export function toNotification(view: components['schemas']['NotificationView']): Notification {
  return {
    type: (view.type as NotificationType | undefined) ?? 'post-liked',
    actorId: view.actorId ?? '',
    subjectPostId: view.subjectPostId ?? null,
    occurredAt: view.occurredAt ?? '',
    read: view.read ?? false,
  };
}

// One row, with the actor and (for like / comment) the post resolved from the two batch
// lookups the page runs per page (ADR-0005). Either can be null if the lookup missed.
export type NotificationCard = Notification & {
  actor: Account | null;
  postMediaId: string | null;
};

// The notification row has no id of its own (#198) — its natural key is the dedup tuple
// the backend stores it under. Stable enough for a React key and for de-duping a page.
export function notificationKey(notification: Notification): string {
  return [
    notification.type,
    notification.actorId,
    notification.subjectPostId ?? '',
    notification.occurredAt,
  ].join('|');
}

const SENTENCE: Record<NotificationType, string> = {
  'post-liked': 'liked your post',
  'post-commented': 'commented on your post',
  'user-followed': 'started following you',
};

export function notificationSentence(type: NotificationType): string {
  return SENTENCE[type];
}

export function postThumbnailUrl(mediaId: string): string {
  return `/api/media/${mediaId}/thumbnail`;
}

// A follow points at the actor's profile; a like / comment points at the post. When the
// piece needed to build the link is missing, the row still renders but isn't a link.
export function notificationHref(card: NotificationCard): string | null {
  if (card.type === 'user-followed') {
    return card.actor?.username ? `/u/${card.actor.username}` : null;
  }
  return card.subjectPostId ? `/p/${card.subjectPostId}` : null;
}
