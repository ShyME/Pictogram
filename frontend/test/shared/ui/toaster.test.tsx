import { Toaster, dismissToast, toast, useToast } from '@shared';
import { act, render, renderHook, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

afterEach(() => {
  const { result } = renderHook(() => useToast());
  act(() => {
    for (const t of result.current.toasts) dismissToast(t.id);
  });
});

describe('Toaster', () => {
  it('renders a queued toast with its title and description', () => {
    render(<Toaster />);
    act(() => {
      toast({ title: 'Posted', description: 'Your photo is live.', variant: 'success' });
    });
    expect(screen.getByText('Posted')).toBeInTheDocument();
    expect(screen.getByText('Your photo is live.')).toBeInTheDocument();
  });

  it('drops a toast when its dismiss control is used', () => {
    render(<Toaster />);
    act(() => {
      toast({ title: 'Link copied' });
    });
    act(() => {
      screen.getByRole('button', { name: 'Dismiss' }).click();
    });
    expect(screen.queryByText('Link copied')).toBeNull();
  });
});
