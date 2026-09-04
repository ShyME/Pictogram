import * as AvatarPrimitive from '@radix-ui/react-avatar';
import type { ComponentProps } from 'react';
import { cn } from '../lib/cn';
import { avatarVariants, type AvatarVariantProps } from './avatarVariants';

type AvatarProps = ComponentProps<typeof AvatarPrimitive.Root> & AvatarVariantProps;

export function Avatar({ className, size, ...props }: AvatarProps) {
  return <AvatarPrimitive.Root className={cn(avatarVariants({ size }), className)} {...props} />;
}

export function AvatarImage({ className, ...props }: ComponentProps<typeof AvatarPrimitive.Image>) {
  return (
    <AvatarPrimitive.Image
      className={cn('aspect-square size-full object-cover', className)}
      {...props}
    />
  );
}

export function AvatarFallback({
  className,
  ...props
}: ComponentProps<typeof AvatarPrimitive.Fallback>) {
  return (
    <AvatarPrimitive.Fallback
      className={cn(
        'flex size-full items-center justify-center font-semibold text-foreground-muted',
        className,
      )}
      {...props}
    />
  );
}
