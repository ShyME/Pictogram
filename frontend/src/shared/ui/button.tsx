import { Slot } from '@radix-ui/react-slot';
import type { ComponentProps } from 'react';
import { cn } from '../lib/cn';
import { buttonVariants, type ButtonVariantProps } from './buttonVariants';
import { Spinner } from './spinner';

type ButtonProps = ComponentProps<'button'> &
  ButtonVariantProps & {
    asChild?: boolean;
    loading?: boolean;
  };

export function Button({
  className,
  variant,
  size,
  asChild = false,
  loading = false,
  disabled,
  children,
  ...props
}: ButtonProps) {
  // A Radix Slot forwards to a single child, so the loader affordance is button-only; an
  // `asChild` link is dimmed and made inert through `aria-disabled` instead.
  const Component = asChild ? Slot : 'button';
  const isInactive = (disabled ?? false) || loading;
  const shouldRenderLoader = loading && !asChild;
  return (
    <Component
      className={cn(buttonVariants({ variant, size }), className)}
      disabled={asChild ? undefined : isInactive}
      aria-disabled={asChild && isInactive ? true : undefined}
      data-loading={shouldRenderLoader || undefined}
      {...props}
    >
      {shouldRenderLoader ? (
        <>
          <Spinner className="size-4 text-current" />
          {children}
        </>
      ) : (
        children
      )}
    </Component>
  );
}

export type { ButtonProps };
