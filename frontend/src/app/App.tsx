import { ChatDock } from '@features/chat';
import { Toaster } from '@shared';
import { RouterProvider } from 'react-router/dom';
import { AppProviders } from './providers';
import { router } from './routes';

export function App() {
  return (
    <AppProviders>
      <RouterProvider router={router} />
      <ChatDock />
      <Toaster />
    </AppProviders>
  );
}
