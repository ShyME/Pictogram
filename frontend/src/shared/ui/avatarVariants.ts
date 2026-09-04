import { cva, type VariantProps } from 'class-variance-authority';

export const avatarVariants = cva(
  'relative flex shrink-0 overflow-hidden rounded-full bg-surface-muted',
  {
    variants: {
      size: {
        sm: 'size-7 text-xs',
        md: 'size-10 text-sm',
        lg: 'size-14 text-base',
      },
    },
    defaultVariants: { size: 'md' },
  },
);

export type AvatarVariantProps = VariantProps<typeof avatarVariants>;
