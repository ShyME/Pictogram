import type { ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { Navigate } from "react-router";
import { SessionExpiredError, fetchFeed } from "./feed-api";

function FeedShell({ children }: { children: ReactNode }) {
  return (
    <main className="mx-auto min-h-dvh max-w-xl bg-neutral-50 px-4 py-10">{children}</main>
  );
}

export function FeedPage() {
  const feed = useQuery({
    queryKey: ["feed"],
    queryFn: fetchFeed,
    // a dead session won't come back by retrying — fail fast to the redirect below
    retry: (count, error) => !(error instanceof SessionExpiredError) && count < 1,
  });

  if (feed.isPending) {
    return (
      <FeedShell>
        <p className="text-center text-sm text-neutral-400">Loading your feed…</p>
      </FeedShell>
    );
  }

  if (feed.isError) {
    if (feed.error instanceof SessionExpiredError) {
      return <Navigate to="/login" replace />;
    }
    return (
      <FeedShell>
        <p className="text-center text-sm text-red-600">
          We couldn&rsquo;t load your feed. Try again in a moment.
        </p>
      </FeedShell>
    );
  }

  // The feed is empty by design until fan-out-on-read lands — the "go find people"
  // nudge is the whole screen for now.
  return (
    <FeedShell>
      <div className="rounded-2xl border border-dashed border-neutral-300 bg-white p-10 text-center">
        <h2 className="text-lg font-semibold text-neutral-900">Your feed is quiet</h2>
        <p className="mx-auto mt-2 max-w-xs text-sm text-neutral-500">
          Find people to follow and their posts will show up here.
        </p>
      </div>
    </FeedShell>
  );
}
