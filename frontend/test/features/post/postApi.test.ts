import { deletePost, fetchPostsByAuthor, publishPost, uploadPhoto } from '@features/post/postApi';
import { SessionExpiredError } from '@shared';
import { jsonResponse, pathOf, problemResponse, stubFetch } from '@test-support/mockFetch';
import { afterEach, expect, test, vi } from 'vitest';

afterEach(() => {
  vi.unstubAllGlobals();
});

test('uploadPhoto posts to the media endpoint and returns the media id', async () => {
  const calls = stubFetch(() => jsonResponse({ mediaId: 'm-1' }, 201));

  await expect(uploadPhoto(new Blob(['x'], { type: 'image/jpeg' }))).resolves.toBe('m-1');
  expect(pathOf(calls[0])).toBe('/api/media');
  expect(calls[0].method).toBe('POST');
});

test('uploadPhoto throws when the upload fails', async () => {
  stubFetch(() => new Response(null, { status: 500 }));

  await expect(uploadPhoto(new Blob(['x']))).rejects.toThrow(/500/);
});

test('uploadPhoto reports an expired session with the shared error', async () => {
  stubFetch(() => problemResponse('unauthorized', 401));

  await expect(uploadPhoto(new Blob(['x']))).rejects.toBeInstanceOf(SessionExpiredError);
});

test('publishPost returns the published post on 201', async () => {
  stubFetch(() =>
    jsonResponse(
      {
        postId: 'p-1',
        authorId: 'u-1',
        mediaId: 'm-1',
        caption: 'hi',
        publishedAt: '2026-09-01T12:00:00Z',
      },
      201,
    ),
  );

  await expect(publishPost({ mediaId: 'm-1', caption: 'hi' })).resolves.toEqual({
    status: 'published',
    post: {
      postId: 'p-1',
      authorId: 'u-1',
      mediaId: 'm-1',
      caption: 'hi',
      publishedAt: '2026-09-01T12:00:00Z',
    },
  });
});

test('publishPost omits a blank caption from the request body', async () => {
  let sentBody: unknown;
  stubFetch(async (request) => {
    sentBody = await request.clone().json();
    return jsonResponse({ postId: 'p-1', mediaId: 'm-1', publishedAt: 't' }, 201);
  });

  await publishPost({ mediaId: 'm-1', caption: ' '.repeat(3) });

  expect(sentBody).toEqual({ mediaId: 'm-1' });
});

test('publishPost maps the unusable-media 422 and the caption 400', async () => {
  stubFetch(() => problemResponse('post-media-unusable', 422));
  await expect(publishPost({ mediaId: 'm-1', caption: '' })).resolves.toEqual({
    status: 'media-unusable',
  });

  stubFetch(() => problemResponse('post-caption-too-long', 400));
  await expect(publishPost({ mediaId: 'm-1', caption: 'x' })).resolves.toEqual({
    status: 'caption-too-long',
  });
});

test('publishPost throws on an unexpected failure', async () => {
  stubFetch(() => new Response(null, { status: 500 }));

  await expect(publishPost({ mediaId: 'm-1', caption: '' })).rejects.toThrow(/500/);
});

test('publishPost reports an expired session with the shared error', async () => {
  stubFetch(() => problemResponse('unauthorized', 401));

  await expect(publishPost({ mediaId: 'm-1', caption: '' })).rejects.toBeInstanceOf(
    SessionExpiredError,
  );
});

test("deletePost issues a DELETE to the post's URL", async () => {
  const calls = stubFetch(() => new Response(null, { status: 204 }));

  await expect(deletePost('p-1')).resolves.toBeUndefined();
  expect(pathOf(calls[0])).toBe('/api/posts/p-1');
  expect(calls[0].method).toBe('DELETE');
});

test('deletePost throws when the server rejects the delete', async () => {
  stubFetch(() => problemResponse('forbidden', 403));

  await expect(deletePost('p-1')).rejects.toThrow(/403/);
});

test("fetchPostsByAuthor requests the author's grid and returns the page", async () => {
  const calls = stubFetch(() =>
    jsonResponse({
      items: [{ postId: 'p-2', authorId: 'u-1', mediaId: 'm-2', publishedAt: 't2' }],
      nextCursor: 'CURSOR',
    }),
  );

  await expect(fetchPostsByAuthor('u-1')).resolves.toEqual({
    posts: [{ postId: 'p-2', authorId: 'u-1', mediaId: 'm-2', caption: null, publishedAt: 't2' }],
    nextCursor: 'CURSOR',
  });
  const url = new URL(calls[0].url);
  expect(url.pathname).toBe('/api/posts');
  expect(url.searchParams.get('author')).toBe('u-1');
});

test('fetchPostsByAuthor passes a cursor and reports the last page as null', async () => {
  const calls = stubFetch(() => jsonResponse({ items: [], nextCursor: null }));

  await expect(fetchPostsByAuthor('u-1', 'CURSOR')).resolves.toEqual({
    posts: [],
    nextCursor: null,
  });
  expect(new URL(calls[0].url).searchParams.get('cursor')).toBe('CURSOR');
});
