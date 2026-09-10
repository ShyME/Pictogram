import { LogOut, Plus, UserRound } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { useMediaQuery } from '../lib/useMediaQuery';
import { Avatar, AvatarFallback } from './avatar';
import { Button } from './button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from './dropdownMenu';

// Tailwind's `md` breakpoint; above it the nav is inline, below it collapses to the
// avatar dropdown. Matched in JS so only one branch is ever in the DOM.
const INLINE_NAV = '(min-width: 48rem)';

type AppNavProps = {
  viewer?: { username: string } | null;
  onSignOut?: () => void;
  signingOut?: boolean;
  // Rendered beside the avatar menu below the inline breakpoint only — where the chat
  // sidebar (#203) is a drawer rather than a docked rail and needs a way in. `AppLayout`
  // passes the trigger; `PublicLayout` never does.
  chatSlot?: ReactNode;
  // The notifications bell (#199) — shown at both breakpoints for a signed-in viewer.
  // `AppLayout` passes it; `PublicLayout` passes it only when someone is signed in.
  notificationsSlot?: ReactNode;
};

export function AppNav({
  viewer,
  onSignOut,
  signingOut = false,
  chatSlot,
  notificationsSlot,
}: AppNavProps) {
  const isInline = useMediaQuery(INLINE_NAV);

  return (
    <header className="border-b border-border bg-surface">
      <nav className="flex items-center justify-between px-4 py-3">
        <Link to="/" className="font-semibold tracking-tight text-foreground">
          Pictogram
        </Link>
        {viewer ? (
          isInline ? (
            <InlineNav
              viewer={viewer}
              onSignOut={onSignOut}
              signingOut={signingOut}
              notificationsSlot={notificationsSlot}
            />
          ) : (
            <div className="flex items-center gap-1">
              {notificationsSlot}
              {chatSlot}
              <AvatarNav viewer={viewer} onSignOut={onSignOut} signingOut={signingOut} />
            </div>
          )
        ) : (
          <Button asChild variant="ghost" size="sm">
            <Link to="/login">Log in</Link>
          </Button>
        )}
      </nav>
    </header>
  );
}

type ViewerNavProps = {
  viewer: { username: string };
  onSignOut?: () => void;
  signingOut: boolean;
};

function InlineNav({
  viewer,
  onSignOut,
  signingOut,
  notificationsSlot,
}: ViewerNavProps & { notificationsSlot?: ReactNode }) {
  return (
    <div className="flex items-center gap-2">
      {notificationsSlot}
      <Button asChild size="sm">
        <Link to="/new">
          <Plus /> New post
        </Link>
      </Button>
      <Button asChild variant="ghost" size="sm">
        <Link to={`/u/${viewer.username}`}>@{viewer.username}</Link>
      </Button>
      <Button variant="secondary" size="sm" onClick={onSignOut} disabled={signingOut}>
        Sign out
      </Button>
    </div>
  );
}

function AvatarNav({ viewer, onSignOut, signingOut }: ViewerNavProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" aria-label="Open menu">
          <Avatar size="sm">
            <AvatarFallback>{viewer.username.slice(0, 2).toUpperCase()}</AvatarFallback>
          </Avatar>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        <DropdownMenuLabel>@{viewer.username}</DropdownMenuLabel>
        <DropdownMenuItem asChild>
          <Link to="/new">
            <Plus /> New post
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <Link to={`/u/${viewer.username}`}>
            <UserRound /> Your profile
          </Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem disabled={signingOut} onSelect={onSignOut}>
          <LogOut /> Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
