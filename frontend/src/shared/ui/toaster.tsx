import {
  Toast,
  ToastClose,
  ToastDescription,
  ToastProvider,
  ToastTitle,
  ToastViewport,
} from './toast';
import { useToast } from './useToast';

const DURATION_MS = 5000;

export function Toaster() {
  const { toasts, dismiss } = useToast();

  return (
    <ToastProvider duration={DURATION_MS} swipeDirection="right">
      {toasts.map(({ id, title, description, variant }) => (
        <Toast
          key={id}
          variant={variant}
          onOpenChange={(isOpen: boolean) => {
            if (!isOpen) dismiss(id);
          }}
        >
          <div className="flex flex-col gap-1">
            {title !== undefined && <ToastTitle>{title}</ToastTitle>}
            {description !== undefined && <ToastDescription>{description}</ToastDescription>}
          </div>
          <ToastClose />
        </Toast>
      ))}
      <ToastViewport />
    </ToastProvider>
  );
}
