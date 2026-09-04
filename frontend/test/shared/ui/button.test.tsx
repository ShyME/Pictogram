import { Button } from '@shared';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

describe('Button', () => {
  it('renders the primary accent variant by default', () => {
    render(<Button>Save</Button>);
    expect(screen.getByRole('button', { name: 'Save' })).toHaveClass('bg-accent');
  });

  it('applies the requested variant and size', () => {
    render(
      <Button variant="danger" size="lg">
        Delete
      </Button>,
    );
    const button = screen.getByRole('button', { name: 'Delete' });
    expect(button).toHaveClass('bg-danger', 'h-11');
  });

  it('shows a spinner and disables itself while loading', () => {
    render(<Button loading>Publishing</Button>);
    const button = screen.getByRole('button', { name: 'Publishing' });
    expect(button).toBeDisabled();
    expect(button.querySelector('svg')).not.toBeNull();
  });

  it('renders as its child element when asChild is set', () => {
    render(
      <Button asChild>
        <a href="/new">New post</a>
      </Button>,
    );
    const link = screen.getByRole('link', { name: 'New post' });
    expect(link).toHaveClass('bg-accent');
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('makes an asChild link inert via aria-disabled rather than the disabled attribute', () => {
    render(
      <Button asChild disabled>
        <a href="/new">New post</a>
      </Button>,
    );
    const link = screen.getByRole('link', { name: 'New post' });
    expect(link).toHaveAttribute('aria-disabled', 'true');
    expect(link).not.toHaveAttribute('disabled');
  });
});
