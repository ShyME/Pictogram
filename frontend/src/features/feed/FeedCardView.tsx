import { relativeTime } from '@shared';
import { MessageCircle } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router';
import type { FeedCard } from './feed';

export function FeedCardView({
  card,
  renderLike,
  onOpenComments,
}: {
  card: FeedCard;
  renderLike?: (postId: string) => ReactNode;
  onOpenComments?: () => void;
}) {
  const handle = card.author.username;
  const name = card.author.displayName;

  return (
    <article className="overflow-hidden rounded-card border border-border bg-surface shadow-card">
      <header className="flex items-baseline gap-2 px-4 py-3">
        {handle ? (
          <Link to={`/u/${handle}`} className="flex items-baseline gap-2 hover:underline">
            {name && <span className="text-sm font-semibold text-foreground">{name}</span>}
            <span className="text-sm text-foreground-muted">@{handle}</span>
          </Link>
        ) : (
          <span className="text-sm font-semibold text-foreground">{name ?? 'Someone'}</span>
        )}
        <time
          dateTime={card.publishedAt}
          className="ml-auto text-xs text-foreground-subtle"
          title={new Date(card.publishedAt).toLocaleString()}
        >
          {relativeTime(card.publishedAt)}
        </time>
      </header>

      <img
        src={card.imageUrl}
        alt={card.caption ?? `A post by ${name ?? (handle ? `@${handle}` : 'someone')}`}
        loading="lazy"
        className="aspect-square w-full bg-surface-muted object-cover"
      />

      <div className="flex items-center gap-4 px-4 pb-1 pt-3">
        {renderLike?.(card.postId)}
        {onOpenComments && (
          <button
            type="button"
            onClick={onOpenComments}
            aria-label="Open comments"
            className="flex items-center gap-2 text-foreground-subtle transition-colors hover:text-foreground-muted"
          >
            <MessageCircle className="size-6" aria-hidden="true" />
            <span className="text-sm">Comment</span>
          </button>
        )}
      </div>

      {card.caption && (
        <p className="px-4 pb-4 pt-2 text-sm text-foreground">
          {handle && <span className="font-semibold text-foreground">@{handle} </span>}
          {card.caption}
        </p>
      )}
      {!card.caption && <div className="pb-3" />}
    </article>
  );
}
