import { ChatOverlay } from './ChatOverlay';
import { useChatConnection } from './useChatConnection';
import { useChatInbox } from './useChatInbox';

// Mounted once, at the router's pathless root layout (app/RootLayout.tsx) rather than
// beside `RouterProvider` in App.tsx, so `ChatOverlay`'s profile link has router context.
// Holds the single session WebSocket, routes every inbound frame, and renders the one
// open conversation overlay.
export function ChatDock() {
  useChatConnection();
  useChatInbox();
  return <ChatOverlay />;
}
