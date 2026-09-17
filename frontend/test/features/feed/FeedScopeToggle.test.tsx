import { FeedScopeToggle } from '@features/feed/FeedScopeToggle';
import { fireEvent, render, screen } from '@testing-library/react';
import { expect, test, vi } from 'vitest';

test('marks Following selected by default and calls back when Explore is picked', () => {
  const onSelectFollowing = vi.fn();
  const onSelectExplore = vi.fn();
  render(
    <FeedScopeToggle
      active="following"
      onSelectFollowing={onSelectFollowing}
      onSelectExplore={onSelectExplore}
    />,
  );

  expect(screen.getByRole('tab', { name: 'Following' })).toHaveAttribute('aria-selected', 'true');
  expect(screen.getByRole('tab', { name: 'Explore' })).toHaveAttribute('aria-selected', 'false');

  fireEvent.click(screen.getByRole('tab', { name: 'Explore' }));
  expect(onSelectExplore).toHaveBeenCalledOnce();
  expect(onSelectFollowing).not.toHaveBeenCalled();
});

test('marks Explore selected when active is explore', () => {
  render(
    <FeedScopeToggle active="explore" onSelectFollowing={vi.fn()} onSelectExplore={vi.fn()} />,
  );

  expect(screen.getByRole('tab', { name: 'Explore' })).toHaveAttribute('aria-selected', 'true');
  expect(screen.getByRole('tab', { name: 'Following' })).toHaveAttribute('aria-selected', 'false');
});
