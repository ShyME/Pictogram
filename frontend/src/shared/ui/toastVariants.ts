import { cva, type VariantProps } from 'class-variance-authority';

export const toastVariants = cva(
  'flex items-start gap-3 rounded-control border border-border border-l-2 bg-surface p-4 text-sm shadow-popover',
  {
    variants: {
      variant: {
        default: 'border-l-accent',
        success: 'border-l-success',
        error: 'border-l-danger',
      },
    },
    defaultVariants: { variant: 'default' },
  },
);

export type ToastVariantProps = VariantProps<typeof toastVariants>;
