import { PictogramMark } from '@shared';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

describe('PictogramMark', () => {
  it('is a labelled image when given a label', () => {
    render(<PictogramMark label="Pictogram" />);
    const mark = screen.getByRole('img', { name: 'Pictogram' });
    expect(mark.tagName.toLowerCase()).toBe('svg');
    expect(mark).not.toHaveAttribute('aria-hidden');
  });

  it('is hidden from the accessibility tree when unlabelled', () => {
    const { container } = render(<PictogramMark />);
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.queryByRole('img')).toBeNull();
  });

  it('keeps its default size while merging a passed className', () => {
    const { container } = render(<PictogramMark className="text-foreground" />);
    expect(container.firstElementChild).toHaveClass('size-6', 'text-foreground');
  });

  it('lets a className override win over the default size', () => {
    const { container } = render(<PictogramMark className="size-9" />);
    const svg = container.firstElementChild;
    expect(svg).toHaveClass('size-9');
    expect(svg).not.toHaveClass('size-6');
  });
});
