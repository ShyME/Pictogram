import '@testing-library/jest-dom/vitest';

if (typeof URL.createObjectURL !== 'function') {
  URL.createObjectURL = () => 'blob:preview';
  URL.revokeObjectURL = () => {
    // no-op: the tests never inspect a revoked object URL
  };
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
