import { ChatDock } from '@features/chat';
import { Outlet } from 'react-router';

// The pathless root route's element: mounted once for the app's whole lifetime, wrapping
// every other route. `ChatDock` lives here (not in `App.tsx`, beside `RouterProvider`) so
// its `ChatOverlay` sits inside router context — the profile-link `<Link>` needs it — while
// still surviving every navigation, including across the AppLayout/PublicLayout split, so
// the session WebSocket it holds doesn't drop.
export function RootLayout() {
  return (
    <>
      <ChatDock />
      <Outlet />
    </>
  );
}
