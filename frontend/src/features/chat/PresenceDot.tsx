import { cn } from '@shared';
import type { Presence } from './usePresence';

// The small online/offline dot beside a chat entry point (ADR-0014). 'unknown' renders
// nothing: chat could not be asked, so it claims neither state.
export function PresenceDot({ presence }: { presence: Presence }) {
  if (presence === 'unknown') return null;
  const isOnline = presence === 'online';
  return (
    <span
      role="img"
      aria-label={isOnline ? 'Online' : 'Offline'}
      className={cn(
        'inline-block size-2 shrink-0 rounded-full',
        isOnline ? 'bg-success' : 'bg-foreground-subtle',
      )}
    />
  );
}
