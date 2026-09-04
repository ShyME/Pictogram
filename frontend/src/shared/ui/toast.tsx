import * as ToastPrimitive from '@radix-ui/react-toast';
import { X } from 'lucide-react';
import type { ComponentProps } from 'react';
import { cn } from '../lib/cn';
import { toastVariants, type ToastVariantProps } from './toastVariants';

export function ToastProvider(props: ComponentProps<typeof ToastPrimitive.Provider>) {
  return <ToastPrimitive.Provider {...props} />;
}

export function ToastViewport({
  className,
  ...props
}: ComponentProps<typeof ToastPrimitive.Viewport>) {
  return (
    <ToastPrimitive.Viewport
      className={cn(
        'fixed bottom-0 right-0 z-[100] flex w-full max-w-sm flex-col gap-2 p-4 outline-none',
        className,
      )}
      {...props}
    />
  );
}

type ToastProps = ComponentProps<typeof ToastPrimitive.Root> & ToastVariantProps;

export function Toast({ className, variant, ...props }: ToastProps) {
  return <ToastPrimitive.Root className={cn(toastVariants({ variant }), className)} {...props} />;
}

export function ToastTitle({ className, ...props }: ComponentProps<typeof ToastPrimitive.Title>) {
  return (
    <ToastPrimitive.Title className={cn('font-semibold text-foreground', className)} {...props} />
  );
}

export function ToastDescription({
  className,
  ...props
}: ComponentProps<typeof ToastPrimitive.Description>) {
  return (
    <ToastPrimitive.Description className={cn('text-foreground-muted', className)} {...props} />
  );
}

export function ToastClose({ className, ...props }: ComponentProps<typeof ToastPrimitive.Close>) {
  return (
    <ToastPrimitive.Close
      className={cn(
        'ml-auto shrink-0 text-foreground-subtle transition-colors hover:text-foreground',
        className,
      )}
      aria-label="Dismiss"
      {...props}
    >
      <X className="size-4" aria-hidden="true" />
    </ToastPrimitive.Close>
  );
}
