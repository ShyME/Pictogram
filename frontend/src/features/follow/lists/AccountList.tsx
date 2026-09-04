import { Avatar, AvatarFallback } from '@shared';
import { Link } from 'react-router';
import type { Account } from '../account';
import { FollowButton } from '../FollowButton';

export function AccountList({ accounts, viewerId }: { accounts: Account[]; viewerId: string }) {
  return (
    <ul className="divide-y divide-border">
      {accounts.map((account) => (
        <li key={account.userId} className="flex items-center gap-3 py-3">
          <Link to={`/u/${account.username}`} className="flex min-w-0 flex-1 items-center gap-3">
            <Avatar>
              <AvatarFallback>{account.username.slice(0, 2).toUpperCase()}</AvatarFallback>
            </Avatar>
            <span className="min-w-0">
              <span className="block truncate text-sm font-medium text-foreground">
                {account.displayName ?? `@${account.username}`}
              </span>
              <span className="block truncate text-sm text-foreground-muted">
                @{account.username}
              </span>
            </span>
          </Link>
          {account.userId !== viewerId && <FollowButton userId={account.userId} />}
        </li>
      ))}
    </ul>
  );
}
