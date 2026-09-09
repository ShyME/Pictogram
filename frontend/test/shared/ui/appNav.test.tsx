import { AppNav } from '@shared';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';

// A stateful matchMedia stand-in: `min-width` queries track `isWide`, and the returned
// setter notifies every live listener so the subscribe/resize path can be exercised.
function installViewport(isInitiallyWide: boolean) {
  const listeners = new Set<() => void>();
  let isWide = isInitiallyWide;

  vi.stubGlobal('matchMedia', (query: string) => ({
    get matches() {
      return isWide && query.includes('min-width');
    },
    media: query,
    onchange: null,
    addListener: (listener: () => void) => listeners.add(listener),
    removeListener: (listener: () => void) => listeners.delete(listener),
    addEventListener: (_: string, listener: () => void) => listeners.add(listener),
    removeEventListener: (_: string, listener: () => void) => listeners.delete(listener),
    dispatchEvent: () => false,
  }));

  return function setViewport(isWideNow: boolean) {
    isWide = isWideNow;
    act(() => {
      for (const listener of listeners) listener();
    });
  };
}

function stubWideViewport(): void {
  installViewport(true);
}

function renderNav(props: Partial<Parameters<typeof AppNav>[0]> = {}) {
  const onSignOut = props.onSignOut ?? vi.fn();
  render(
    <MemoryRouter>
      <AppNav viewer={{ username: 'ada' }} onSignOut={onSignOut} {...props} />
    </MemoryRouter>,
  );
  return { onSignOut };
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('at md and wider', () => {
  it('shows the inline actions: new post, the viewer handle, sign out', () => {
    stubWideViewport();
    renderNav();

    expect(screen.getByRole('link', { name: /new post/i })).toHaveAttribute('href', '/new');
    expect(screen.getByRole('link', { name: '@ada' })).toHaveAttribute('href', '/u/ada');
    expect(screen.getByRole('button', { name: /sign out/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /open menu/i })).not.toBeInTheDocument();
  });

  it('signs out when the inline control is clicked', () => {
    stubWideViewport();
    const { onSignOut } = renderNav();

    fireEvent.click(screen.getByRole('button', { name: /sign out/i }));

    expect(onSignOut).toHaveBeenCalledOnce();
  });

  it('disables the inline sign-out while a sign-out is in flight', () => {
    stubWideViewport();
    renderNav({ signingOut: true });

    expect(screen.getByRole('button', { name: /sign out/i })).toBeDisabled();
  });
});

describe('below md', () => {
  it('collapses the actions behind the avatar menu', () => {
    renderNav();

    expect(screen.queryByRole('link', { name: /new post/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /sign out/i })).not.toBeInTheDocument();

    fireEvent.keyDown(screen.getByRole('button', { name: /open menu/i }), { key: 'Enter' });

    const menu = screen.getByRole('menu');
    expect(within(menu).getByRole('menuitem', { name: /new post/i })).toHaveAttribute(
      'href',
      '/new',
    );
    expect(within(menu).getByRole('menuitem', { name: /your profile/i })).toHaveAttribute(
      'href',
      '/u/ada',
    );
    expect(within(menu).getByRole('menuitem', { name: /sign out/i })).toBeInTheDocument();
  });

  it('signs out from the menu item', () => {
    const { onSignOut } = renderNav();

    fireEvent.keyDown(screen.getByRole('button', { name: /open menu/i }), { key: 'Enter' });
    fireEvent.click(screen.getByRole('menuitem', { name: /sign out/i }));

    expect(onSignOut).toHaveBeenCalledOnce();
  });
});

describe('chat slot', () => {
  it('renders the slot beside the avatar menu below md', () => {
    renderNav({ chatSlot: <button type="button">chat entry</button> });

    expect(screen.getByRole('button', { name: 'chat entry' })).toBeInTheDocument();
  });

  it('does not render the slot in the inline nav at md and wider', () => {
    stubWideViewport();
    renderNav({ chatSlot: <button type="button">chat entry</button> });

    expect(screen.queryByRole('button', { name: 'chat entry' })).not.toBeInTheDocument();
  });

  it('renders nothing extra when no slot is given', () => {
    renderNav();

    expect(screen.getByRole('button', { name: /open menu/i })).toBeInTheDocument();
  });
});

describe('anonymous visitor', () => {
  it('shows only a log-in link, no viewer actions', () => {
    stubWideViewport();
    render(
      <MemoryRouter>
        <AppNav viewer={null} />
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: /log in/i })).toHaveAttribute('href', '/login');
    expect(screen.queryByRole('link', { name: /new post/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /sign out/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /open menu/i })).not.toBeInTheDocument();
  });
});

it('swaps layout when the viewport crosses the breakpoint', () => {
  const setViewport = installViewport(false);
  renderNav();

  expect(screen.getByRole('button', { name: /open menu/i })).toBeInTheDocument();

  setViewport(true);
  expect(screen.getByRole('button', { name: /sign out/i })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /open menu/i })).not.toBeInTheDocument();

  setViewport(false);
  expect(screen.getByRole('button', { name: /open menu/i })).toBeInTheDocument();
});
