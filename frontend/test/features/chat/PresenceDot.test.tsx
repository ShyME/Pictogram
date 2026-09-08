import { PresenceDot } from '@features/chat/PresenceDot';
import { render, screen } from '@testing-library/react';
import { expect, test } from 'vitest';

test('shows an online dot', () => {
  render(<PresenceDot presence="online" />);
  expect(screen.getByRole('img', { name: 'Online' })).toBeInTheDocument();
});

test('shows an offline dot', () => {
  render(<PresenceDot presence="offline" />);
  expect(screen.getByRole('img', { name: 'Offline' })).toBeInTheDocument();
});

test('renders nothing while presence is unknown', () => {
  const { container } = render(<PresenceDot presence="unknown" />);
  expect(container).toBeEmptyDOMElement();
});
