import {
  type NotificationCard,
  notificationHref,
  notificationKey,
  notificationSentence,
  toNotification,
} from '@features/notifications/notification';
import { expect, test } from 'vitest';

const card = (over: Partial<NotificationCard>): NotificationCard => ({
  type: 'post-liked',
  actorId: 'u-1',
  subjectPostId: 'p-1',
  occurredAt: '2026-09-04T10:00:00Z',
  read: false,
  actor: { userId: 'u-1', username: 'ada', displayName: 'Ada' },
  postMediaId: 'm-1',
  ...over,
});

test('toNotification normalises the wire shape and defaults', () => {
  expect(
    toNotification({ type: 'user-followed', actorId: 'u-9', occurredAt: 't', read: true }),
  ).toEqual({
    type: 'user-followed',
    actorId: 'u-9',
    subjectPostId: null,
    occurredAt: 't',
    read: true,
  });
  expect(toNotification({}).type).toBe('post-liked');
  expect(toNotification({}).read).toBe(false);
});

test('the sentence matches the type', () => {
  expect(notificationSentence('post-liked')).toBe('liked your post');
  expect(notificationSentence('post-commented')).toBe('commented on your post');
  expect(notificationSentence('user-followed')).toBe('started following you');
});

test('a like or comment links to the post, a follow links to the actor profile', () => {
  expect(notificationHref(card({ type: 'post-liked', subjectPostId: 'p-7' }))).toBe('/p/p-7');
  expect(notificationHref(card({ type: 'post-commented', subjectPostId: 'p-7' }))).toBe('/p/p-7');
  expect(notificationHref(card({ type: 'user-followed' }))).toBe('/u/ada');
});

test('a row with no piece to build its link is not a link', () => {
  expect(notificationHref(card({ type: 'post-liked', subjectPostId: null }))).toBeNull();
  expect(notificationHref(card({ type: 'user-followed', actor: null }))).toBeNull();
});

test('the row key is the dedup tuple, so two distinct notifications never collide', () => {
  const like = card({ type: 'post-liked' });
  const comment = card({ type: 'post-commented' });
  expect(notificationKey(like)).not.toBe(notificationKey(comment));
  expect(notificationKey(like)).toBe(notificationKey(card({ read: true })));
});
