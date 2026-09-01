import { Link } from "react-router";
import type { Account } from "../model/account";
import { FollowButton } from "./FollowButton";

/**
 * The rows of a follower / following list (#57): each account's avatar placeholder, name and
 * `@handle`, linking through to its profile, with a follow / unfollow control on every row
 * but the viewer's own.
 */
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
