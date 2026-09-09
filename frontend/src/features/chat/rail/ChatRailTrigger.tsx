import { Button } from '@shared';
import { MessagesSquare } from 'lucide-react';
import { openRailDrawer, useIsRailDrawerOpen } from './railStore';

// The narrow-viewport way into the rail: an `AppNav` icon that opens the drawer. On a
// docked viewport the rail is always visible and `AppNav` never renders this. (#204 badges
// it when a row is unread.)
export function ChatRailTrigger() {
  const isOpen = useIsRailDrawerOpen();
  return (
    <Button
      variant="ghost"
      size="icon"
      aria-label="Open messages"
      aria-expanded={isOpen}
      onClick={openRailDrawer}
    >
      <MessagesSquare />
    </Button>
  );
}
