import { MessageButton } from '@features/chat';
import { closeConversation, openConversationPeer } from '@features/chat/chatStore';
import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, expect, test } from 'vitest';

afterEach(() => {
  closeConversation();
});

test('opens a conversation for the given peer when clicked', () => {
  const peer = { userId: 'u-ada', username: 'ada', displayName: 'Ada' };
  render(<MessageButton peer={peer} />);

  fireEvent.click(screen.getByRole('button', { name: 'Message' }));

  expect(openConversationPeer()).toEqual(peer);
});
