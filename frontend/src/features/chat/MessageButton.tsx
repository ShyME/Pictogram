import { Button } from '@shared';
import { PresenceDot } from './PresenceDot';
import { type ChatPeer, openConversation } from './chatStore';
import { usePresence } from './usePresence';

// The profile screen's entry point into chat (ADR-0014): it already holds the peer's
// profile, so it hands it straight in — no lookup, chat stays profile-ignorant. The dot
// asks chat whether the peer is reachable for live delivery, on show (#167).
export function MessageButton({ peer }: { peer: ChatPeer }) {
  const presence = usePresence(peer.userId);
  return (
    <span className="inline-flex items-center gap-1.5">
      <Button
        variant="secondary"
        size="sm"
        onClick={() => {
          openConversation(peer);
        }}
      >
        Message
      </Button>
      <PresenceDot presence={presence} />
    </span>
  );
}
