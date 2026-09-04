import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { cn } from '../lib/cn';

type EmptyStateProps = {
  icon?: LucideIcon;
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
};

export function EmptyState({ icon: Icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div className={cn('flex flex-col items-center gap-2 px-4 py-10 text-center', className)}>
      {Icon && (
        <div className="mb-1 grid size-11 place-items-center rounded-control bg-surface-muted">
          <Icon className="size-5 text-foreground-muted" aria-hidden="true" />
        </div>
      )}
      <p className="font-semibold text-foreground">{title}</p>
      {description !== undefined && (
        <p className="max-w-xs text-sm text-foreground-muted">{description}</p>
      )}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
