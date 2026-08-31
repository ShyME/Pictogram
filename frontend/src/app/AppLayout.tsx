import { Outlet, useLoaderData } from "react-router";
import { SignOutButton } from "@features/auth";
import type { Profile } from "@features/profile";

export function AppLayout() {
  // rootLoader returns { profile } for anyone who reaches this layout (it redirects
  // everyone else away before the element renders).
  const { profile } = useLoaderData() as { profile: Profile };

  return (
    <div className="min-h-dvh bg-neutral-50">
      <header className="flex items-center justify-between border-b border-neutral-200 bg-white px-4 py-3">
        <span className="font-semibold tracking-tight text-neutral-900">Pictogram</span>
        <div className="flex items-center gap-3 text-sm text-neutral-500">
          <span>@{profile.username}</span>
          <SignOutButton />
        </div>
      </header>
      <Outlet />
    </div>
  );
}
