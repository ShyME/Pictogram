import { Modal, ModalContent, ModalTitle } from '@shared';
import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';
import { expect, test, vi } from 'vitest';

function Harness({
  onOpenChange,
  closeOnEscape,
}: {
  onOpenChange?: (isOpen: boolean) => void;
  closeOnEscape?: boolean;
}) {
  const [isOpen, setIsOpen] = useState(true);
  return (
    <Modal
      isOpen={isOpen}
      onOpenChange={(next) => {
        setIsOpen(next);
        onOpenChange?.(next);
      }}
    >
      <ModalContent closeOnEscape={closeOnEscape}>
        <ModalTitle>Delete this post?</ModalTitle>
        <button type="button">Confirm</button>
      </ModalContent>
    </Modal>
  );
}

test('renders a labelled modal dialog and locks body scroll while open', () => {
  const { unmount } = render(<Harness />);

  const dialog = screen.getByRole('dialog');
  expect(dialog).toHaveAttribute('aria-modal', 'true');
  expect(dialog).toHaveAccessibleName('Delete this post?');
  expect(document.body.style.overflow).toBe('hidden');

  unmount();
  expect(document.body.style.overflow).toBe('');
});

test('Escape closes the modal', () => {
  const onOpenChange = vi.fn();
  render(<Harness onOpenChange={onOpenChange} />);

  fireEvent.keyDown(document, { key: 'Escape' });

  expect(onOpenChange).toHaveBeenCalledWith(false);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test('closeOnEscape={false} keeps the modal open on Escape', () => {
  render(<Harness closeOnEscape={false} />);

  fireEvent.keyDown(document, { key: 'Escape' });

  expect(screen.getByRole('dialog')).toBeInTheDocument();
});

test('a click on the backdrop closes, a click inside does not', () => {
  const onOpenChange = vi.fn();
  render(<Harness onOpenChange={onOpenChange} />);

  fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));
  expect(onOpenChange).not.toHaveBeenCalled();

  const backdrop = screen.getByRole('dialog').parentElement;
  if (!backdrop) throw new Error('expected a backdrop');
  fireEvent.click(backdrop);
  expect(onOpenChange).toHaveBeenCalledWith(false);
});
