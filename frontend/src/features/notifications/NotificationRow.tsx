import { Avatar, AvatarFallback, cn, relativeTime } from '@shared';
import { Link } from 'react-router';
import {
  type NotificationCard,
  notificationHref,
  notificationSentence,
  postThumbnailUrl,
} from './notification';

export function NotificationRow({ card }: { card: NotificationCard }) {
  const href = notificationHref(card);
  const name = card.actor?.displayName ?? (card.actor?.username ? `@${card.actor.username}` : null);
  const initials = card.actor?.username.slice(0, 2).toUpperCase() ?? '?';

  const body = (
    <>
      <Avatar size="sm" className="shrink-0">
        <AvatarFallback>{initials}</AvatarFallback>
      </Avatar>
      <span className="min-w-0 flex-1 text-sm text-foreground">
        <span className="font-semibold">{name ?? 'Someone'}</span> {notificationSentence(card.type)}
        <time
          dateTime={card.occurredAt}
          title={new Date(card.occurredAt).toLocaleString()}
          className="ml-1.5 whitespace-nowrap text-xs text-foreground-subtle"
        >
          {relativeTime(card.occurredAt)}
        </time>
      </span>
      {card.postMediaId && (
        <img
          src={postThumbnailUrl(card.postMediaId)}
          alt=""
          loading="lazy"
          className="size-11 shrink-0 rounded-sm bg-surface-muted object-cover"
        />
      )}
    </>
  );

  const className = cn(
    'flex items-center gap-3 px-4 py-3',
    !card.read && 'bg-accent/5',
    href && 'transition-colors hover:bg-surface-muted',
  );

  return (
    <li>
      {href ? (
        <Link to={href} className={className}>
          {body}
        </Link>
      ) : (
        <div className={className}>{body}</div>
      )}
    </li>
  );
}
