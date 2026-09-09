import { Button, EmptyState, Input, Spinner, cn, useMediaQuery } from '@shared';
import { MessageCircleOff, PanelLeftClose, PanelLeftOpen, X } from 'lucide-react';
import { type ReactNode, useEffect, useMemo, useRef, useState } from 'react';
import { openConversation } from '../chatStore';
import { usePresences } from '../usePresences';
import { RailRow } from './RailRow';
import { orderPeers, railName } from './orderPeers';
import {
  closeRailDrawer,
  toggleRailCollapsed,
  useIsRailCollapsed,
  useIsRailDrawerOpen,
} from './railStore';
import { useFollowingPeers } from './useFollowingPeers';

// Matches the nav's inline breakpoint: at and above it the rail is a docked column, below
// it a drawer opened from the `AppNav` chat icon.
const RAIL_DOCKED = '(min-width: 48rem)';

// The persistent messaging rail (#203): everyone the viewer follows, with live presence,
// online-first. Mounted once in `AppLayout` — authed screens only.
export function ChatRail({ viewerId }: { viewerId: string }) {
  const isDocked = useMediaQuery(RAIL_DOCKED);
  return isDocked ? <DockedRail viewerId={viewerId} /> : <RailDrawer viewerId={viewerId} />;
}

function DockedRail({ viewerId }: { viewerId: string }) {
  const isCollapsed = useIsRailCollapsed();

  // The outer cell stretches to the row's full height (so the divider and surface run the
  // length of the page); the inner `<aside>` is what sticks and scrolls.
  return (
    <div
      className={cn('shrink-0 border-r border-border bg-surface', isCollapsed ? 'w-12' : 'w-72')}
    >
      <aside
        aria-label="Messages"
        className={cn(
          'sticky top-0 flex h-dvh flex-col overflow-hidden',
          isCollapsed && 'items-center py-2.5',
        )}
      >
        {isCollapsed ? (
          <Button
            variant="ghost"
            size="icon"
            aria-label="Expand messages"
            onClick={toggleRailCollapsed}
          >
            <PanelLeftOpen />
          </Button>
        ) : (
          <>
            <RailHeader
              action={
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label="Collapse messages"
                  onClick={toggleRailCollapsed}
                >
                  <PanelLeftClose />
                </Button>
              }
            />
            <RailBody viewerId={viewerId} />
          </>
        )}
      </aside>
    </div>
  );
}

function RailDrawer({ viewerId }: { viewerId: string }) {
  const isOpen = useIsRailDrawerOpen();
  const panelRef = useRef<HTMLElement>(null);

  // Open drawer: lock the body scroll (CSSOM property, not a `<style>` — CSP-safe, same
  // as the shared Modal), move focus into the panel, close on Escape, and hand focus back
  // to whatever was focused before on close.
  useEffect(() => {
    if (!isOpen) return;
    const previouslyFocused = document.activeElement as HTMLElement | null;
    const { body } = document;
    const restoreOverflow = body.style.overflow;
    body.style.overflow = 'hidden';
    panelRef.current?.focus();

    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') closeRailDrawer();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      body.style.overflow = restoreOverflow;
      previouslyFocused?.focus();
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex bg-foreground/20"
      onClick={(event) => {
        if (event.target === event.currentTarget) closeRailDrawer();
      }}
    >
      <aside
        ref={panelRef}
        aria-label="Messages"
        tabIndex={-1}
        className="flex h-full w-80 max-w-[85vw] flex-col overflow-hidden border-r border-border bg-surface shadow-dialog outline-none"
      >
        <RailHeader
          action={
            <Button
              variant="ghost"
              size="icon"
              aria-label="Close messages"
              onClick={closeRailDrawer}
            >
              <X />
            </Button>
          }
        />
        <RailBody viewerId={viewerId} onNavigate={closeRailDrawer} />
      </aside>
    </div>
  );
}

function RailHeader({ action }: { action: ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b border-border py-2 pl-3 pr-2">
      <h2 className="text-sm font-semibold text-foreground">Messages</h2>
      {action}
    </div>
  );
}

function RailNote({ children }: { children: ReactNode }) {
  return <p className="px-3 py-6 text-center text-sm text-foreground-subtle">{children}</p>;
}

function RailBody({ viewerId, onNavigate }: { viewerId: string; onNavigate?: () => void }) {
  const { peers, status } = useFollowingPeers(viewerId);
  const peerIds = useMemo(() => peers.map((peer) => peer.userId), [peers]);
  const presences = usePresences(peerIds);
  const [filter, setFilter] = useState('');

  const visible = useMemo(() => {
    const needle = filter.trim().toLowerCase();
    const matched = needle
      ? peers.filter(
          (peer) =>
            railName(peer).toLowerCase().includes(needle) ||
            peer.username.toLowerCase().includes(needle),
        )
      : peers;
    return orderPeers(matched, presences);
  }, [peers, presences, filter]);

  if (status === 'loading') {
    return (
      <div className="flex flex-1 items-center justify-center py-6">
        <Spinner label="Loading conversations" />
      </div>
    );
  }

  if (status === 'error') {
    return <RailNote>We couldn&rsquo;t load your follows. Try again in a moment.</RailNote>;
  }

  if (peers.length === 0) {
    return (
      <EmptyState
        icon={MessageCircleOff}
        title="No one to message yet"
        description="Follow people to start a conversation with them here."
      />
    );
  }

  return (
    <>
      <div className="border-b border-border p-2">
        <Input
          type="search"
          aria-label="Filter conversations"
          placeholder="Filter"
          value={filter}
          onChange={(event) => {
            setFilter(event.target.value);
          }}
        />
      </div>
      {visible.length === 0 ? (
        <RailNote>No matches for &ldquo;{filter.trim()}&rdquo;.</RailNote>
      ) : (
        <ul className="min-h-0 flex-1 divide-y divide-border overflow-y-auto">
          {visible.map((peer) => (
            <RailRow
              key={peer.userId}
              peer={peer}
              presence={presences.get(peer.userId) ?? 'unknown'}
              onOpen={() => {
                openConversation(peer);
                onNavigate?.();
              }}
            />
          ))}
        </ul>
      )}
    </>
  );
}
