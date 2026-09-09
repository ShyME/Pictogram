import { Button } from '@shared';
import { MessagesSquare } from 'lucide-react';
import { useIsAnyPeerUnread } from '../unreadStore';
import { openRailDrawer, useIsRailDrawerOpen } from './railStore';

// The narrow-viewport way into the rail: an `AppNav` icon that opens the drawer. On a
// docked viewport the rail is always visible and `AppNav` never renders this. A badge
// shows while any followed peer has an unread message (#204).
export function ChatRailTrigger() {
  const isOpen = useIsRailDrawerOpen();
  const hasUnread = useIsAnyPeerUnread();
  return (
    <span className="relative inline-flex">
      <Button
        variant="ghost"
        size="icon"
        aria-label="Open messages"
        aria-expanded={isOpen}
        onClick={openRailDrawer}
      >
        <MessagesSquare />
      </Button>
      {hasUnread && (
        <span
          role="img"
          aria-label="Unread messages"
          className="pointer-events-none absolute right-1.5 top-1.5 size-2 rounded-full bg-accent ring-2 ring-surface"
        />
      )}
    </span>
  );
}
