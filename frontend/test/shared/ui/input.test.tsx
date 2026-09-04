import { Input, Textarea } from '@shared';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

describe('Input / Textarea', () => {
  it('forwards native props and stays a text input by default', () => {
    render(<Input aria-label="Username" defaultValue="ansel" />);
    const input = screen.getByLabelText('Username');
    expect(input).toHaveAttribute('type', 'text');
    expect(input).toHaveValue('ansel');
  });

  it('carries an error affordance through aria-invalid', () => {
    render(<Input aria-label="Handle" aria-invalid />);
    expect(screen.getByLabelText('Handle')).toHaveClass('aria-[invalid=true]:border-danger');
  });

  it('renders a textarea element', () => {
    render(<Textarea aria-label="Caption" />);
    expect(screen.getByLabelText('Caption').tagName).toBe('TEXTAREA');
  });
});
