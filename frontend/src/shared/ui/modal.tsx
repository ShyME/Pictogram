import {
  type ComponentProps,
  createContext,
  type KeyboardEvent as ReactKeyboardEvent,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useId,
  useRef,
} from 'react';
import { createPortal } from 'react-dom';
import { cn } from '../lib/cn';

// Hand-rolled, not the Radix `Dialog`: Radix's scroll-lock injects a `<style>` element the
// CSP (`style-src 'self'`, ADR-0011) blocks. The lock here sets `body.style.overflow`
// through the CSSOM property API, which `style-src` does not govern (ADR-0011 decision 3).

type ModalContextValue = {
  onOpenChange: (isOpen: boolean) => void;
  labelId: string;
  descriptionId: string;
};

const ModalContext = createContext<ModalContextValue | null>(null);

function useModalContext(): ModalContextValue {
  const context = useContext(ModalContext);
  if (!context) throw new Error('Modal subcomponents must be rendered inside <Modal>');
  return context;
}

type ModalProps = {
  isOpen: boolean;
  onOpenChange: (isOpen: boolean) => void;
  children: ReactNode;
};

export function Modal({ isOpen, onOpenChange, children }: ModalProps) {
  const labelId = useId();
  const descriptionId = useId();

  if (!isOpen) return null;

  return (
    <ModalContext.Provider value={{ onOpenChange, labelId, descriptionId }}>
      {createPortal(children, document.body)}
    </ModalContext.Provider>
  );
}

const FOCUSABLE =
  'a[href],button:not([disabled]),textarea:not([disabled]),input:not([disabled]),select:not([disabled]),[tabindex]:not([tabindex="-1"])';

type ModalContentProps = ComponentProps<'div'> & {
  role?: 'dialog' | 'alertdialog';
  closeOnEscape?: boolean;
};

export function ModalContent({
  className,
  children,
  role = 'dialog',
  closeOnEscape = true,
  ...props
}: ModalContentProps) {
  const { onOpenChange, labelId, descriptionId } = useModalContext();
  const panelRef = useRef<HTMLDivElement>(null);

  const close = useCallback(() => {
    onOpenChange(false);
  }, [onOpenChange]);

  useEffect(() => {
    const previouslyFocused = document.activeElement as HTMLElement | null;
    const { body } = document;
    const restoreOverflow = body.style.overflow;
    body.style.overflow = 'hidden';
    panelRef.current?.focus();

    return () => {
      body.style.overflow = restoreOverflow;
      previouslyFocused?.focus();
    };
  }, []);

  useEffect(() => {
    if (!closeOnEscape) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') close();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
    };
  }, [closeOnEscape, close]);

  const trapTab = (event: ReactKeyboardEvent<HTMLDivElement>) => {
    const panel = panelRef.current;
    if (!panel || event.key !== 'Tab') return;

    const focusable = [...panel.querySelectorAll<HTMLElement>(FOCUSABLE)].filter(
      (node) => node.offsetParent !== null,
    );
    if (focusable.length === 0) {
      event.preventDefault();
      return;
    }
    const first = focusable[0];
    const last = focusable.at(-1) ?? first;
    const active = document.activeElement;
    const isAtStart = active === panel || active === first;
    const isAtEnd = active === last;
    if (isAtStart && event.shiftKey) {
      event.preventDefault();
      last.focus();
    } else if (isAtEnd && !event.shiftKey) {
      event.preventDefault();
      first.focus();
    }
  };

  const onOverlayClick = (event: ReactMouseEvent<HTMLDivElement>) => {
    if (event.target === event.currentTarget) close();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-foreground/40 p-4"
      onClick={onOverlayClick}
    >
      <div
        ref={panelRef}
        role={role}
        aria-modal="true"
        aria-labelledby={labelId}
        aria-describedby={descriptionId}
        tabIndex={-1}
        onKeyDown={trapTab}
        className={cn(
          'relative w-full max-w-md rounded-dialog border border-border bg-surface p-6 shadow-dialog outline-none',
          className,
        )}
        {...props}
      >
        {children}
      </div>
    </div>
  );
}

export function ModalHeader({ className, ...props }: ComponentProps<'div'>) {
  return <div className={cn('flex flex-col gap-1.5', className)} {...props} />;
}

export function ModalFooter({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      className={cn('mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end', className)}
      {...props}
    />
  );
}

export function ModalTitle({ className, ...props }: ComponentProps<'h2'>) {
  const { labelId } = useModalContext();
  return (
    <h2
      id={labelId}
      className={cn('text-lg font-semibold tracking-tight text-foreground', className)}
      {...props}
    />
  );
}

export function ModalDescription({ className, ...props }: ComponentProps<'p'>) {
  const { descriptionId } = useModalContext();
  return (
    <p id={descriptionId} className={cn('text-sm text-foreground-muted', className)} {...props} />
  );
}
