import { accessTokenChanges } from '@shared';
import { useEffect, useState } from 'react';
import { chatConnectionStatus, type ChatConnectionStatus } from './chatConnection';

// Mount once at the app shell (alongside AppNav, #135) — one subscription is one
// WebSocket connection for the whole signed-in session, independent of which screen is
// active.
export function useChatConnection(): ChatConnectionStatus {
  const [status, setStatus] = useState<ChatConnectionStatus>('closed');

  useEffect(() => {
    const subscription = chatConnectionStatus(accessTokenChanges()).subscribe(setStatus);
    return () => {
      subscription.unsubscribe();
    };
  }, []);

  return status;
}
