import type { ComponentProps } from 'react';
import { cn } from '../lib/cn';

export function Textarea({ className, ...props }: ComponentProps<'textarea'>) {
  return (
    <textarea
      className={cn(
        'min-h-20 w-full rounded-control border border-border-strong bg-surface px-3 py-2 text-sm text-foreground shadow-xs outline-none transition-colors',
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
