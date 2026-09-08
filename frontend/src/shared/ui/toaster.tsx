import {
  Toast,
  ToastAction,
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
      {toasts.map(({ id, title, description, variant, action }) => (
        <Toast
          key={id}
          variant={variant}
          onOpenChange={(isOpen: boolean) => {
            if (!isOpen) dismiss(id);
          }}
        >
          <div className="flex min-w-0 flex-col gap-1">
            {title !== undefined && <ToastTitle>{title}</ToastTitle>}
            {description !== undefined && <ToastDescription>{description}</ToastDescription>}
          </div>
          {action && (
            <ToastAction className="ml-auto" altText={action.label} onClick={action.onClick}>
              {action.label}
            </ToastAction>
          )}
          <ToastClose />
        </Toast>
      ))}
      <ToastViewport />
    </ToastProvider>
  );
}
