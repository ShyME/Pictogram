import { LoaderCircle } from 'lucide-react';
import { cn } from '../lib/cn';

const sizes = { sm: 'size-4', md: 'size-6', lg: 'size-8' } as const;

type SpinnerProps = {
  size?: keyof typeof sizes;
  className?: string;
  // Accessible label announced to screen readers; omit for a purely decorative spinner.
  label?: string;
};

export function Spinner({ size = 'md', className, label }: SpinnerProps) {
  return (
    <LoaderCircle
      role={label ? 'status' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      className={cn('animate-spin text-foreground-muted', sizes[size], className)}
    />
  );
}
