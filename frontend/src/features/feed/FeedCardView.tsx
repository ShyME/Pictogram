import type { ReactNode } from 'react';
import { Link } from 'react-router';
import type { FeedCard } from './feed';
import { relativeTime } from './relativeTime';

export function FeedCardView({
  card,
  renderLike,
}: {
  card: FeedCard;
  renderLike?: (postId: string) => ReactNode;
}) {
  const handle = card.author.username;
  const name = card.author.displayName;

  return (
    <article className="overflow-hidden rounded-2xl border border-neutral-200 bg-white">
      <header className="flex items-baseline gap-2 px-4 py-3">
        {handle ? (
          <Link to={`/u/${handle}`} className="flex items-baseline gap-2 hover:underline">
            {name && <span className="text-sm font-semibold text-neutral-900">{name}</span>}
            <span className="text-sm text-neutral-500">@{handle}</span>
          </Link>
        ) : (
          <span className="text-sm font-semibold text-neutral-900">{name ?? 'Someone'}</span>
        )}
        <time
          dateTime={card.publishedAt}
          className="ml-auto text-xs text-neutral-400"
          title={new Date(card.publishedAt).toLocaleString()}
        >
          {relativeTime(card.publishedAt)}
        </time>
      </header>

      <img
        src={card.imageUrl}
        alt={card.caption ?? `A post by ${name ?? (handle ? `@${handle}` : 'someone')}`}
        loading="lazy"
        className="aspect-square w-full bg-neutral-100 object-cover"
      />

      <div className="flex items-center gap-2 px-4 pb-1 pt-3 text-neutral-400">
        {renderLike?.(card.postId)}
      </div>

      {card.caption && (
        <p className="px-4 pb-4 pt-2 text-sm text-neutral-800">
          {handle && <span className="font-semibold text-neutral-900">@{handle} </span>}
          {card.caption}
        </p>
      )}
      {!card.caption && <div className="pb-3" />}
    </article>
  );
}
