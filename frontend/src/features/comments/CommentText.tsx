import { Fragment } from 'react';
import { commentSegments } from './comment';

// `@mention`s are deliberately not parsed (#137) — only bare URLs become links.
export function CommentText({ body }: { body: string }) {
  return (
    <>
      {commentSegments(body).map((segment, index) =>
        segment.href ? (
          <a
            key={index}
            href={segment.href}
            target="_blank"
            rel="noopener noreferrer nofollow"
            className="text-accent underline-offset-2 hover:underline"
          >
            {segment.text}
          </a>
        ) : (
          <Fragment key={index}>{segment.text}</Fragment>
        ),
      )}
    </>
  );
}
