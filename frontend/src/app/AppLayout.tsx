import { useSignOut } from '@features/auth';
import { ChatRail, ChatRailTrigger } from '@features/chat';
import type { Profile } from '@features/profile';
import { AppNav } from '@shared';
import { Outlet, useLoaderData } from 'react-router';
import type { rootLoader } from './guards';

export type AppLayoutContext = { profile: Profile };

export function AppLayout() {
  const { profile } = useLoaderData<typeof rootLoader>();
  const { signOut, signingOut } = useSignOut();

  return (
    <div className="flex min-h-dvh flex-col bg-canvas">
      <AppNav
        viewer={{ username: profile.username }}
        onSignOut={signOut}
        signingOut={signingOut}
        chatSlot={<ChatRailTrigger />}
      />
      <div className="flex flex-1">
        <ChatRail viewerId={profile.userId} />
        <div className="min-w-0 flex-1">
          <Outlet context={{ profile } satisfies AppLayoutContext} />
        </div>
      </div>
    </div>
  );
}
