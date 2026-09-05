import { useSignOut } from '@features/auth';
import { useChatConnection } from '@features/chat';
import type { Profile } from '@features/profile';
import { AppNav } from '@shared';
import { Outlet, useLoaderData } from 'react-router';
import type { rootLoader } from './guards';

export type AppLayoutContext = { profile: Profile };

export function AppLayout() {
  const { profile } = useLoaderData<typeof rootLoader>();
  const { signOut, signingOut } = useSignOut();
  useChatConnection();

  return (
    <div className="min-h-dvh bg-canvas">
      <AppNav viewer={{ username: profile.username }} onSignOut={signOut} signingOut={signingOut} />
      <Outlet context={{ profile } satisfies AppLayoutContext} />
    </div>
  );
}
