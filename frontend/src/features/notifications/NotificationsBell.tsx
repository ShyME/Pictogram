import { Button } from '@shared';
import { Bell } from 'lucide-react';
import { Link } from 'react-router';
import { useUnreadCount } from './useUnreadCount';

// The bell sits in `AppNav` on every authed screen (#199). It polls the unread count and
// caps the badge at "9+"; a zero count shows the bell alone.
export function NotificationsBell() {
  const { data: count = 0 } = useUnreadCount();
  const label = count > 0 ? `Notifications, ${count} unread` : 'Notifications';

  return (
    <span className="relative inline-flex">
      <Button asChild variant="ghost" size="icon" aria-label={label}>
        <Link to="/notifications">
          <Bell />
        </Link>
      </Button>
      {count > 0 && (
        <span
          aria-hidden
          className="pointer-events-none absolute -right-0.5 -top-0.5 flex min-w-4 items-center justify-center rounded-full bg-accent px-1 text-[0.625rem] font-semibold leading-4 text-accent-foreground ring-2 ring-surface"
        >
          {count > 9 ? '9+' : count}
        </span>
      )}
    </span>
  );
}
