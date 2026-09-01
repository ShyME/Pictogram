import { Link } from "react-router";
import type { Account } from "../account";
import { FollowButton } from "../FollowButton";

export function AccountList({ accounts, viewerId }: { accounts: Account[]; viewerId: string }) {
  return (
    <ul className="divide-y divide-neutral-200">
      {accounts.map((account) => (
        <li key={account.userId} className="flex items-center gap-3 py-3">
          <Link
            to={`/u/${account.username}`}
            className="flex min-w-0 flex-1 items-center gap-3"
          >
            <span aria-hidden className="size-10 shrink-0 rounded-full bg-neutral-200" />
            <span className="min-w-0">
              <span className="block truncate text-sm font-medium text-neutral-900">
                {account.displayName ?? `@${account.username}`}
              </span>
              <span className="block truncate text-sm text-neutral-500">@{account.username}</span>
            </span>
          </Link>
          {account.userId !== viewerId && <FollowButton userId={account.userId} />}
        </li>
      ))}
    </ul>
  );
}
