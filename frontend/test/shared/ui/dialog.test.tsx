import { Dialog, DialogContent, DialogDescription, DialogTitle, DialogTrigger } from '@shared';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

describe('Dialog', () => {
  it('opens from its trigger and shows a titled dialog', () => {
    render(
      <Dialog>
        <DialogTrigger>Open</DialogTrigger>
        <DialogContent>
          <DialogTitle>Delete this post?</DialogTitle>
          <DialogDescription>This cannot be undone.</DialogDescription>
        </DialogContent>
      </Dialog>,
    );

    expect(screen.queryByRole('dialog')).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Open' }));

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAccessibleName('Delete this post?');
    expect(dialog).toHaveAccessibleDescription('This cannot be undone.');
  });

  it('closes from the built-in close button', () => {
    render(
      <Dialog defaultOpen>
        <DialogContent>
          <DialogTitle>Settings</DialogTitle>
        </DialogContent>
      </Dialog>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(screen.queryByRole('dialog')).toBeNull();
  });
});
