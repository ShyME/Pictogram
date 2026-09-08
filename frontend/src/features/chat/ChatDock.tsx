import { ChatOverlay } from './ChatOverlay';
import { useChatConnection } from './useChatConnection';
import { useChatInbox } from './useChatInbox';

// Mounted once at the app shell (App.tsx, beside the Toaster): holds the single session
// WebSocket, routes every inbound frame, and renders the one open conversation overlay.
export function ChatDock() {
  useChatConnection();
  useChatInbox();
  return <ChatOverlay />;
}
