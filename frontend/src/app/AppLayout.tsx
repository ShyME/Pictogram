import { Link, Outlet, useLoaderData } from "react-router";
import { SignOutButton } from "@features/auth";
import type { Profile } from "@features/profile";

export function AppLayout() {
  // rootLoader returns { profile } for anyone who reaches this layout (it redirects
  // everyone else away before the element renders).
  const { profile } = useLoaderData() as { profile: Profile };

  return (
    <div className="min-h-dvh bg-neutral-50">
      <header className="flex items-center justify-between border-b border-neutral-200 bg-white px-4 py-3">
        <Link to="/" className="font-semibold tracking-tight text-neutral-900">
          Pictogram
        </Link>
        <div className="flex items-center gap-3 text-sm text-neutral-500">
          <Link
            to="/new"
            className="rounded-lg bg-neutral-900 px-3 py-1.5 font-medium text-white hover:bg-neutral-700"
          >
            New post
          </Link>
          <Link
            to={`/u/${profile.username}`}
            className="font-medium text-neutral-700 hover:text-neutral-900"
          >
            @{profile.username}
          </Link>
          <SignOutButton />
        </div>
      </header>
      <Outlet />
    </div>
  );
}
