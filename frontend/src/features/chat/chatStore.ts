import { useSyncExternalStore } from 'react';

// Chat resolves display data itself (ADR-0014) — the caller opening a conversation already
// holds the peer's profile, so it passes it straight in.
export type ChatPeer = { userId: string; username: string; displayName: string | null };

export type ChatMessage = {
  id: string;
  direction: 'outgoing' | 'incoming';
  text: string;
  // Meaningful only for outgoing: `false` once chat has reported it undelivered, or once
  // it could not be sent at all. Incoming messages are delivered by definition.
  delivered: boolean;
};

// The one open conversation (ADR-0014): opening another replaces it, and it holds no
// history — closing discards it, reopening starts empty.
export type Conversation = { peer: ChatPeer; messages: ChatMessage[] };

let conversation: Conversation | null = null;
let counter = 0;
const listeners = new Set<() => void>();

function notify(): void {
  for (const listener of listeners) listener();
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function append(message: Omit<ChatMessage, 'id'>): void {
  if (!conversation) return;
  counter += 1;
  conversation = {
    ...conversation,
    messages: [...conversation.messages, { ...message, id: `msg-${counter.toString()}` }],
  };
  notify();
}

export function openConversation(peer: ChatPeer): void {
  if (conversation?.peer.userId === peer.userId) return;
  conversation = { peer, messages: [] };
  notify();
}

export function closeConversation(): void {
  if (!conversation) return;
  conversation = null;
  notify();
}

export function openConversationPeer(): ChatPeer | null {
  return conversation?.peer ?? null;
}

export function recordSentMessage(text: string, wasDelivered: boolean): void {
  append({ direction: 'outgoing', text, delivered: wasDelivered });
}

export function recordReceivedMessage(text: string): void {
  append({ direction: 'incoming', text, delivered: true });
}

// Chat echoes the text of an undelivered message back (OutboundEvent) — match it to the
// most recent still-delivered outgoing message and flip it.
export function markMessageUndelivered(text: string): void {
  if (!conversation) return;
  const messages = [...conversation.messages];
  for (let index = messages.length - 1; index >= 0; index -= 1) {
    const message = messages[index];
    if (message.direction === 'outgoing' && message.text === text && message.delivered) {
      messages[index] = { ...message, delivered: false };
      conversation = { ...conversation, messages };
      notify();
      return;
    }
  }
}

export function useConversation(): Conversation | null {
  return useSyncExternalStore(
    subscribe,
    () => conversation,
    () => conversation,
  );
}
