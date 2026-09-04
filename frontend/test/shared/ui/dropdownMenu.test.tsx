import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@shared';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

function Menu({ onPick }: { onPick: () => void }) {
  return (
    <DropdownMenu open>
      <DropdownMenuTrigger>Account</DropdownMenuTrigger>
      <DropdownMenuContent>
        <DropdownMenuItem>Your profile</DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={onPick}>Sign out</DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

describe('DropdownMenu', () => {
  it('exposes its items with menu semantics when open', () => {
    render(<Menu onPick={vi.fn()} />);
    expect(screen.getByRole('menu')).toBeInTheDocument();
    expect(screen.getAllByRole('menuitem')).toHaveLength(2);
  });

  it('fires onSelect when an item is chosen', () => {
    const onPick = vi.fn();
    render(<Menu onPick={onPick} />);
    fireEvent.click(screen.getByRole('menuitem', { name: 'Sign out' }));
    expect(onPick).toHaveBeenCalledOnce();
  });
});
