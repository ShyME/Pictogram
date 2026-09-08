import { fetchAccounts, toast } from '@shared';
import { useEffect } from 'react';
import { chatEvents } from './chatConnection';
import {
  markMessageUndelivered,
  openConversation,
  openConversationPeer,
  recordReceivedMessage,
} from './chatStore';

// A message for a conversation the overlay isn't currently showing must not be silently
// missed (ADR-0014) — surface it as a toast, resolving the sender's name client-side since
// the frame carries only a userId. When the sender resolves, the toast opens the
// conversation (seeded with this message) on tap.
async function toastIncoming(senderUserId: string, text: string): Promise<void> {
  const accounts = await fetchAccounts([senderUserId]).catch(() => null);
  const sender = accounts?.get(senderUserId);
  if (!sender) {
    toast({ title: 'New message', description: text });
    return;
  }
  toast({
    title: `New message from ${sender.displayName ?? `@${sender.username}`}`,
    description: text,
    action: {
      label: 'Open',
      onClick: () => {
        openConversation(sender, text);
      },
    },
  });
}

// Mounted once at the app shell: the single reader of chat's inbound stream, routing each
// frame to the open conversation or to a toast.
export function useChatInbox(): void {
  useEffect(() => {
    const subscription = chatEvents().subscribe((event) => {
      const peer = openConversationPeer();
      if (event.type === 'message') {
        if (event.senderUserId === peer?.userId) recordReceivedMessage(event.text);
        else void toastIncoming(event.senderUserId, event.text);
      } else if (event.recipientUserId === peer?.userId) {
        markMessageUndelivered(event.text);
      }
    });
    return () => {
      subscription.unsubscribe();
    };
  }, []);
}
