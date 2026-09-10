import { useSignOut } from '@features/auth';
import { NotificationsBell } from '@features/notifications';
import type { Profile } from '@features/profile';
import { AppNav } from '@shared';
import { Outlet, useLoaderData } from 'react-router';
import type { viewerLoader } from './guards';

export type PublicLayoutContext = { viewer: Profile | null };

export function PublicLayout() {
  const { viewer } = useLoaderData<typeof viewerLoader>();
  const { signOut, signingOut } = useSignOut();

  return (
    <div className="min-h-dvh bg-canvas">
      <AppNav
        viewer={viewer ? { username: viewer.username } : null}
        onSignOut={signOut}
        signingOut={signingOut}
        notificationsSlot={viewer ? <NotificationsBell /> : undefined}
      />
      <Outlet context={{ viewer } satisfies PublicLayoutContext} />
    </div>
  );
}
