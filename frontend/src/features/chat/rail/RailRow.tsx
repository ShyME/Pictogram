import { Avatar, AvatarFallback } from '@shared';
import { PresenceDot } from '../PresenceDot';
import type { ChatPeer } from '../chatStore';
import type { Presence } from '../usePresence';
import { railName } from './orderPeers';

// One followed person in the rail. Clicking it opens the conversation with the peer this
// row already holds — chat never looks a profile up (ADR-0014).
export function RailRow({
  peer,
  presence,
  onOpen,
}: {
  peer: ChatPeer;
  presence: Presence;
  onOpen: () => void;
}) {
  return (
    <li>
      <button
        type="button"
        onClick={onOpen}
        className="flex w-full items-center gap-3 px-3 py-2.5 text-left transition-colors hover:bg-surface-muted"
      >
        <Avatar size="sm">
          <AvatarFallback>{peer.username.slice(0, 2).toUpperCase()}</AvatarFallback>
        </Avatar>
        <span className="min-w-0 flex-1">
          <span className="flex items-center gap-1.5">
            <span className="truncate text-sm font-medium text-foreground">{railName(peer)}</span>
            <PresenceDot presence={presence} />
          </span>
          <span className="block truncate text-xs text-foreground-muted">@{peer.username}</span>
        </span>
      </button>
    </li>
  );
}
