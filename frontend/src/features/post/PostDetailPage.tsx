import {
  type Account,
  Avatar,
  AvatarFallback,
  Button,
  Spinner,
  fetchAccounts,
  fetchPosts,
  relativeTime,
} from '@shared';
import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router';
import { originalUrl } from './post';
import { postDetailKey } from './queryKeys';

function Shell({ children }: { children: ReactNode }) {
  return <main className="mx-auto max-w-lg px-4 py-8">{children}</main>;
}

export function PostDetailPage({
  renderLike,
  renderComments,
}: {
  renderLike?: (postId: string) => ReactNode;
  renderComments?: (postId: string, authorId: string) => ReactNode;
} = {}) {
  const { postId = '' } = useParams();

  const query = useQuery({
    queryKey: postDetailKey(postId),
    queryFn: async () => {
      const posts = await fetchPosts([postId]);
      const post = posts.get(postId);
      if (!post) return null;
      // The batch profile read needs a token; a signed-out deep-link still shows the post,
      // just without the author's name.
      const authors = await fetchAccounts([post.authorId]).catch(
        (): Map<string, Account> => new Map(),
      );
      return { post, author: authors.get(post.authorId) ?? null };
    },
  });

  if (query.isPending) {
    return (
      <Shell>
        <div className="flex justify-center py-10">
          <Spinner label="Loading the post" />
        </div>
      </Shell>
    );
  }

  if (query.isError) {
    return (
      <Shell>
        <p className="py-10 text-center text-sm text-danger-text">
          We couldn&rsquo;t load this post. Try again in a moment.
        </p>
      </Shell>
    );
  }

  if (query.data === null) {
    return (
      <Shell>
        <div className="py-16 text-center">
          <h1 className="text-lg font-semibold text-foreground">This post doesn&rsquo;t exist</h1>
          <p className="mt-2 text-sm text-foreground-muted">
            The link may be wrong, or the post may have been deleted.
          </p>
          <Button variant="link" asChild className="mt-6">
            <Link to="/">Back to Pictogram</Link>
          </Button>
        </div>
      </Shell>
    );
  }

  const { post, author } = query.data;
  const handle = author?.username;
  const name = author?.displayName ?? (handle ? `@${handle}` : 'Someone');

  return (
    <Shell>
      <article className="overflow-hidden rounded-card border border-border bg-surface shadow-card">
        <header className="flex items-center gap-2 px-4 py-3">
          {handle ? (
            <Link
              to={`/u/${handle}`}
              className="flex min-w-0 items-center gap-2 text-sm font-semibold text-foreground hover:underline"
            >
              <Avatar size="sm">
                <AvatarFallback>{handle.slice(0, 2).toUpperCase()}</AvatarFallback>
              </Avatar>
              <span className="truncate">{name}</span>
            </Link>
          ) : (
            <span className="text-sm font-semibold text-foreground">{name}</span>
          )}
          <time
            dateTime={post.publishedAt}
            title={new Date(post.publishedAt).toLocaleString()}
            className="ml-auto shrink-0 text-xs text-foreground-subtle"
          >
            {relativeTime(post.publishedAt)}
          </time>
        </header>

        <img
          src={originalUrl(post.mediaId)}
          alt={post.caption ?? `A post by ${name}`}
          className="aspect-square w-full bg-surface-muted object-cover"
        />

        <div className="flex items-center gap-4 px-4 pb-1 pt-3">{renderLike?.(post.postId)}</div>

        {post.caption && (
          <p className="break-words px-4 pb-4 pt-2 text-sm text-foreground">
            {handle && <span className="font-semibold">@{handle} </span>}
            {post.caption}
          </p>
        )}

        {renderComments && (
          <div className="border-t border-border px-4 py-4">
            {renderComments(post.postId, post.authorId)}
          </div>
        )}
      </article>
    </Shell>
  );
}
