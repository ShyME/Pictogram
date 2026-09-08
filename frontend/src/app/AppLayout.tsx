import { useSignOut } from '@features/auth';
import type { Profile } from '@features/profile';
import { AppNav } from '@shared';
import { Outlet, useLoaderData } from 'react-router';
import type { rootLoader } from './guards';

export type AppLayoutContext = { profile: Profile };

export function AppLayout() {
  const { profile } = useLoaderData<typeof rootLoader>();
  const { signOut, signingOut } = useSignOut();

  return (
    <div className="min-h-dvh bg-canvas">
      <AppNav viewer={{ username: profile.username }} onSignOut={signOut} signingOut={signingOut} />
      <Outlet context={{ profile } satisfies AppLayoutContext} />
    </div>
  );
}
