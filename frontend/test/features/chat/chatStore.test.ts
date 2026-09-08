import {
  closeConversation,
  openConversation,
  openConversationPeer,
  recordSentMessage,
} from '@features/chat/chatStore';
import { afterEach, expect, test } from 'vitest';

const ada = { userId: 'u-ada', username: 'ada', displayName: 'Ada' };
const bob = { userId: 'u-bob', username: 'bob', displayName: null };

// The store is a module singleton — reset it between tests.
afterEach(() => {
  closeConversation();
});

test('opening a conversation for a second peer replaces the first', () => {
  openConversation(ada);
  openConversation(bob);

  expect(openConversationPeer()).toEqual(bob);
});

test('reopening the currently open peer is a no-op, not a reset', () => {
  openConversation(ada);
  recordSentMessage('still here', true);

  openConversation({ ...ada });

  expect(openConversationPeer()).toEqual(ada);
});

test('closing discards the conversation', () => {
  openConversation(ada);
  closeConversation();

  expect(openConversationPeer()).toBeNull();
});

test('messages are only recorded while a conversation is open', () => {
  recordSentMessage('into the void', true);

  expect(openConversationPeer()).toBeNull();
});
