import '@testing-library/jest-dom/vitest';

if (typeof URL.createObjectURL !== 'function') {
  URL.createObjectURL = () => 'blob:preview';
  URL.revokeObjectURL = () => {
    // no-op: the tests never inspect a revoked object URL
  };
}

function noop(): void {
  // stand-in for browser APIs jsdom doesn't implement; nothing here inspects the result
}

if (typeof matchMedia !== 'function') {
  // jsdom declares matchMedia but leaves it undefined; default every query to "no match"
  // so a component that gates layout on a breakpoint renders its narrow branch. Tests
  // that need the wide layout stub `matchMedia` themselves.
  Object.defineProperty(globalThis, 'matchMedia', {
    value: (query: string): MediaQueryList =>
      ({
        matches: false,
        media: query,
        onchange: null,
        addListener: noop,
        removeListener: noop,
        addEventListener: noop,
        removeEventListener: noop,
        dispatchEvent: () => false,
      }) as MediaQueryList,
    configurable: true,
    writable: true,
  });
}

if (typeof Element !== 'undefined' && typeof Element.prototype.scrollIntoView !== 'function') {
  // jsdom ships no scrollIntoView; Radix menus call it on the item they focus when opened.
  Element.prototype.scrollIntoView = noop;
}

if (!('IntersectionObserver' in globalThis)) {
  class NoopIntersectionObserver implements IntersectionObserver {
    readonly root = null;
    readonly rootMargin = '';
    readonly thresholds: readonly number[] = [];
    observe(): void {
      // no-op
    }
    unobserve(): void {
      // no-op
    }
    disconnect(): void {
      // no-op
    }
    takeRecords(): IntersectionObserverEntry[] {
      return [];
    }
  }
  Object.defineProperty(globalThis, 'IntersectionObserver', {
    value: NoopIntersectionObserver,
    configurable: true,
    writable: true,
  });
}
