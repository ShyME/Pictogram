import { useSyncExternalStore } from 'react';

export type ToastVariant = 'default' | 'success' | 'error';

// An optional single call to action rendered as a button in the toast; clicking it also
// dismisses the toast.
export type ToastAction = { label: string; onClick: () => void };

export type ToastRecord = {
  id: string;
  title?: string;
  description?: string;
  variant: ToastVariant;
  action?: ToastAction;
};

type ToastInput = Omit<ToastRecord, 'id' | 'variant'> & { variant?: ToastVariant };

const MAX_QUEUED = 4;

let records: ToastRecord[] = [];
const listeners = new Set<() => void>();
let counter = 0;

function notify(): void {
  for (const listener of listeners) listener();
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

// Queue a toast. Returns its id; it clears itself after the Toaster's duration.
export function toast(input: ToastInput): string {
  counter += 1;
  const id = `toast-${counter.toString()}`;
  const record: ToastRecord = {
    id,
    title: input.title,
    description: input.description,
    variant: input.variant ?? 'default',
    action: input.action,
  };
  records = [...records, record].slice(-MAX_QUEUED);
  notify();
  return id;
}

export function dismissToast(id: string): void {
  records = records.filter((record) => record.id !== id);
  notify();
}

export function useToast(): {
  toasts: ToastRecord[];
  toast: typeof toast;
  dismiss: typeof dismissToast;
} {
  const toasts = useSyncExternalStore(
    subscribe,
    () => records,
    () => records,
  );
  return { toasts, toast, dismiss: dismissToast };
}
