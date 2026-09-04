import { cn } from '@shared';
import { describe, expect, it } from 'vitest';

describe('cn', () => {
  it('lets a later default utility win over an earlier conflicting one', () => {
    expect(cn('px-2', 'px-4')).toBe('px-4');
  });

  it('dedupes conflicts on the semantic @theme radius / shadow / colour keys', () => {
    expect(cn('rounded-card', 'rounded-lg')).toBe('rounded-lg');
    expect(cn('shadow-card', 'shadow-none')).toBe('shadow-none');
    expect(cn('bg-surface', 'bg-canvas')).toBe('bg-canvas');
    expect(cn('text-foreground-muted', 'text-current')).toBe('text-current');
  });

  it('keeps non-conflicting classes and drops falsy input', () => {
    const off = 0 as number;
    expect(cn('rounded-card', 'bg-surface', off > 0 && 'hidden')).toBe('rounded-card bg-surface');
  });
});
