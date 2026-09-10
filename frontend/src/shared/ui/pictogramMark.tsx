import type { ComponentProps } from 'react';
import { cn } from '../lib/cn';

type PictogramMarkProps = ComponentProps<'svg'> & {
  // Accessible name, making the mark a standalone image; omit it beside the visible
  // "Pictogram" wordmark, where the mark is decorative.
  label?: string;
};

// The brand mark: a viewfinder cropping a "P" whose bowl is flat-topped (the "D-bowl").
// Crop-marks take `currentColor`; the P is always the accent.
export function PictogramMark({ label, className, ...props }: PictogramMarkProps) {
  return (
    <svg
      viewBox="0 0 96 96"
      fill="none"
      role={label ? 'img' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      className={cn('size-6', className)}
      {...props}
    >
      <g stroke="currentColor" strokeWidth={7} strokeLinecap="round" strokeLinejoin="round">
        <path d="M9 27V17a8 8 0 0 1 8-8h10" />
        <path d="M69 9h10a8 8 0 0 1 8 8v10" />
        <path d="M87 69v10a8 8 0 0 1-8 8h-10" />
        <path d="M27 87H17a8 8 0 0 1-8-8V69" />
      </g>
      <rect x="34" y="26" width="10" height="44" rx="3" className="fill-accent" />
      <path d="M35 29.5h18a11.5 11.5 0 0 1 0 23H35" strokeWidth={7} className="stroke-accent" />
    </svg>
  );
}
