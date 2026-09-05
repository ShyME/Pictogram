import { useSignOut } from '@features/auth';
import { AppNav } from '@shared';
import { Outlet, useLoaderData } from 'react-router';
import type { viewerLoader } from './guards';

export function PublicLayout() {
  const { viewer } = useLoaderData<typeof viewerLoader>();
  const { signOut, signingOut } = useSignOut();

  return (
    <div className="min-h-dvh bg-canvas">
      <AppNav
        viewer={viewer ? { username: viewer.username } : null}
        onSignOut={signOut}
        signingOut={signingOut}
      />
      <Outlet />
    </div>
  );
}
