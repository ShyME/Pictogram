import { clsx, type ClassValue } from 'clsx';
import { extendTailwindMerge } from 'tailwind-merge';

// tailwind-merge only knows Tailwind's default scale; teach it the semantic @theme keys
// (index.css) so a consumer's `className` override of `rounded-card` / `shadow-card` /
// `bg-surface` wins over a component's base utility instead of both surviving.
const twMerge = extendTailwindMerge({
  extend: {
    theme: {
      color: [
        'canvas',
        'surface',
        'surface-muted',
        'foreground',
        'foreground-muted',
        'foreground-subtle',
        'border',
        'border-strong',
        'accent',
        'accent-hover',
        'accent-foreground',
        'success',
        'danger',
        'danger-hover',
        'danger-foreground',
        'danger-text',
        'danger-surface',
        'danger-border',
        'ring',
        'like',
      ],
      radius: ['control', 'card', 'dialog'],
      shadow: ['card', 'popover', 'dialog'],
    },
  },
});

// Merge class lists, letting a later Tailwind utility win over an earlier conflicting one.
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
