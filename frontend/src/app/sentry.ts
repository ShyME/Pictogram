import * as Sentry from '@sentry/react';

// A no-op locally and in CI, where no DSN is baked into the build (ADR-0016). Sentry's
// default GlobalHandlers integration already reports uncaught errors and unhandled
// rejections; RouteError covers what the router's own error boundary catches instead.
export function initSentry() {
  const dsn = import.meta.env.VITE_SENTRY_DSN;
  if (!dsn) return;

  Sentry.init({ dsn, environment: 'production' });
}
