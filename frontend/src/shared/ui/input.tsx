import type { ComponentProps } from 'react';
import { cn } from '../lib/cn';

export function Input({ className, type = 'text', ...props }: ComponentProps<'input'>) {
  return (
    <input
      type={type}
      className={cn(
        'h-9 w-full rounded-control border border-border-strong bg-surface px-3 text-sm text-foreground shadow-xs outline-none transition-colors',
        'placeholder:text-foreground-subtle',
        'focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/25',
        'disabled:cursor-not-allowed disabled:opacity-50',
        'aria-[invalid=true]:border-danger aria-[invalid=true]:ring-danger/25',
        className,
      )}
      {...props}
    />
  );
}
