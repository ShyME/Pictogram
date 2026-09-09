import { useMediaQuery } from '@shared';
import { act, render, screen } from '@testing-library/react';
import { afterEach, expect, test, vi } from 'vitest';

function installMatchMedia(isInitiallyMatching: boolean) {
  const listeners = new Set<() => void>();
  let isMatching = isInitiallyMatching;
  vi.stubGlobal('matchMedia', (query: string) => ({
    get matches() {
      return isMatching;
    },
    media: query,
    onchange: null,
    addListener: (l: () => void) => listeners.add(l),
    removeListener: (l: () => void) => listeners.delete(l),
    addEventListener: (_: string, l: () => void) => listeners.add(l),
    removeEventListener: (_: string, l: () => void) => listeners.delete(l),
    dispatchEvent: () => false,
  }));
  return (isMatchingNow: boolean) => {
    isMatching = isMatchingNow;
    act(() => {
      for (const l of listeners) l();
    });
  };
}

function Probe() {
  return <p>{useMediaQuery('(min-width: 48rem)') ? 'wide' : 'narrow'}</p>;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

test('reports the current match', () => {
  installMatchMedia(true);
  render(<Probe />);
  expect(screen.getByText('wide')).toBeInTheDocument();
});

test('updates when the query starts or stops matching', () => {
  const setMatches = installMatchMedia(false);
  render(<Probe />);
  expect(screen.getByText('narrow')).toBeInTheDocument();

  setMatches(true);
  expect(screen.getByText('wide')).toBeInTheDocument();

  setMatches(false);
  expect(screen.getByText('narrow')).toBeInTheDocument();
});
