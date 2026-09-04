import { Spinner } from '@shared';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

describe('Spinner', () => {
  it('is a labelled status when given a label', () => {
    render(<Spinner label="Loading feed" />);
    expect(screen.getByRole('status', { name: 'Loading feed' })).toBeInTheDocument();
  });

  it('is hidden from the accessibility tree when unlabelled', () => {
    const { container } = render(<Spinner />);
    expect(container.querySelector('[aria-hidden="true"]')).not.toBeNull();
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('maps its size prop to a dimension class', () => {
    const { container } = render(<Spinner size="lg" />);
    expect(container.firstElementChild).toHaveClass('size-8');
  });
});
