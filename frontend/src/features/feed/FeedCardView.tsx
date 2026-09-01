import { Link } from "react-router";
import { relativeTime } from "./relative-time";
import type { FeedCard } from "./feed";

export function FeedCardView({ card }: { card: FeedCard }) {
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
          <span className="text-sm font-semibold text-neutral-900">{name ?? "Someone"}</span>
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
        alt={card.caption ?? `A post by ${name ?? (handle ? `@${handle}` : "someone")}`}
        loading="lazy"
        className="aspect-square w-full bg-neutral-100 object-cover"
      />

      <div className="flex items-center gap-2 px-4 pb-1 pt-3 text-neutral-400">
        <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 1 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />
        </svg>
        <span className="text-sm">0 likes</span>
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
