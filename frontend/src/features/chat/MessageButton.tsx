import { Button } from '@shared';
import { type ChatPeer, openConversation } from './chatStore';

// The profile screen's entry point into chat (ADR-0014): it already holds the peer's
// profile, so it hands it straight in — no lookup, chat stays profile-ignorant.
export function MessageButton({ peer }: { peer: ChatPeer }) {
  return (
    <Button
      variant="secondary"
      size="sm"
      onClick={() => {
        openConversation(peer);
      }}
    >
      Message
    </Button>
  );
}
