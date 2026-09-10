import { useQuery } from '@tanstack/react-query';
import { fetchUnreadCount } from './notificationsApi';
import { unreadCountKey } from './queryKeys';

const POLL_INTERVAL_MS = 30_000;

// The bell polls the unread count every 30s and on window focus (#199). A failed poll
// leaves the last known count in place — the badge is not worth a visible error.
export function useUnreadCount() {
  return useQuery({
    queryKey: unreadCountKey(),
    queryFn: fetchUnreadCount,
    refetchInterval: POLL_INTERVAL_MS,
    refetchIntervalInBackground: false,
    refetchOnWindowFocus: true,
    staleTime: 0,
    retry: false,
  });
}
