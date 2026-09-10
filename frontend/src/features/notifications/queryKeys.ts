export function notificationsKey() {
  return ['notifications', 'list'] as const;
}

export function unreadCountKey() {
  return ['notifications', 'unread-count'] as const;
}
