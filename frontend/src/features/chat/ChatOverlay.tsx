import { Avatar, AvatarFallback, Button, Textarea, cn } from '@shared';
import { Send, X } from 'lucide-react';
import { type KeyboardEvent, useState } from 'react';
import { sendChatMessage } from './chatConnection';
import {
  type ChatMessage,
  type ChatPeer,
  type Conversation,
  closeConversation,
  recordSentMessage,
  useConversation,
} from './chatStore';

function peerName(peer: ChatPeer): string {
  return peer.displayName ?? `@${peer.username}`;
}

function Bubble({ message }: { message: ChatMessage }) {
  const isOutgoing = message.direction === 'outgoing';
  return (
    <div className={cn('flex', isOutgoing ? 'justify-end' : 'justify-start')}>
      <div className="flex min-w-0 max-w-[85%] flex-col gap-0.5">
        <p
          className={cn(
            'whitespace-pre-wrap break-words rounded-card px-3 py-2 text-sm',
            isOutgoing ? 'bg-accent text-accent-foreground' : 'bg-surface-muted text-foreground',
          )}
        >
          {message.text}
        </p>
        {isOutgoing && !message.delivered && (
          <span className="px-1 text-xs text-danger-text">Not delivered</span>
        )}
      </div>
    </div>
  );
}

function OpenConversation({ conversation }: { conversation: Conversation }) {
  const { peer, messages } = conversation;
  const name = peerName(peer);
  const [draft, setDraft] = useState('');

  const send = () => {
    const text = draft.trim();
    if (!text) return;
    recordSentMessage(text, sendChatMessage(peer.userId, text) === 'sent');
    setDraft('');
  };

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter' || event.shiftKey) return;
    event.preventDefault();
    send();
  };

  return (
    <div className="fixed inset-x-0 bottom-0 z-40 sm:inset-x-auto sm:bottom-4 sm:right-4">
      <section
        role="dialog"
        aria-modal="false"
        aria-label={`Chat with ${name}`}
        className="flex h-[70dvh] max-h-[30rem] w-full flex-col overflow-hidden rounded-t-dialog border border-border bg-surface shadow-dialog sm:h-[28rem] sm:w-80 sm:rounded-dialog"
      >
        <header className="flex items-center gap-3 border-b border-border p-3">
          <Avatar size="sm">
            <AvatarFallback>{peer.username.slice(0, 2).toUpperCase()}</AvatarFallback>
          </Avatar>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold text-foreground">{name}</p>
            <p className="truncate text-xs text-foreground-muted">@{peer.username}</p>
          </div>
          <Button variant="ghost" size="icon" aria-label="Close chat" onClick={closeConversation}>
            <X />
          </Button>
        </header>

        <div className="flex flex-1 flex-col gap-2 overflow-y-auto p-3">
          {messages.length === 0 ? (
            <p className="m-auto text-center text-sm text-foreground-subtle">
              No messages yet. Say hello.
            </p>
          ) : (
            messages.map((message) => <Bubble key={message.id} message={message} />)
          )}
        </div>

        <form
          onSubmit={(event) => {
            event.preventDefault();
            send();
          }}
          className="flex items-end gap-2 border-t border-border p-3"
        >
          <Textarea
            aria-label={`Message ${name}`}
            placeholder="Write a message…"
            className="max-h-24 min-h-9 flex-1 resize-none"
            rows={1}
            value={draft}
            onChange={(event) => {
              setDraft(event.target.value);
            }}
            onKeyDown={onKeyDown}
          />
          <Button type="submit" size="icon" aria-label="Send" disabled={!draft.trim()}>
            <Send />
          </Button>
        </form>
      </section>
    </div>
  );
}

// One transient overlay at a time (ADR-0014). Keyed by peer so switching conversations
// resets the composer and never carries a draft across.
export function ChatOverlay() {
  const conversation = useConversation();
  if (!conversation) return null;
  return <OpenConversation key={conversation.peer.userId} conversation={conversation} />;
}
