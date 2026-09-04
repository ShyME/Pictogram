import { dismissToast, toast, useToast } from '@shared';
import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';

afterEach(() => {
  const { result } = renderHook(() => useToast());
  act(() => {
    for (const t of result.current.toasts) dismissToast(t.id);
  });
});

describe('useToast', () => {
  it('queues a toast and exposes it to subscribers', () => {
    const { result } = renderHook(() => useToast());
    act(() => {
      toast({ title: 'Posted', variant: 'success' });
    });
    expect(result.current.toasts).toEqual([
      expect.objectContaining({ title: 'Posted', variant: 'success' }),
    ]);
  });

  it('defaults the variant', () => {
    const { result } = renderHook(() => useToast());
    act(() => {
      toast({ description: 'Link copied' });
    });
    expect(result.current.toasts[0]?.variant).toBe('default');
  });

  it('dismisses by id', () => {
    const { result } = renderHook(() => useToast());
    let id = '';
    act(() => {
      id = toast({ title: 'One' });
      toast({ title: 'Two' });
    });
    act(() => {
      result.current.dismiss(id);
    });
    expect(result.current.toasts.map((t) => t.title)).toEqual(['Two']);
  });
});
