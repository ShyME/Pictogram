import { EmptyState } from '@shared';
import { render, screen } from '@testing-library/react';
import { ImageIcon } from 'lucide-react';
import { describe, expect, it } from 'vitest';

describe('EmptyState', () => {
  it('renders the title and description', () => {
    render(<EmptyState title="No posts yet" description="Follow people to see their work." />);
    expect(screen.getByText('No posts yet')).toBeInTheDocument();
    expect(screen.getByText('Follow people to see their work.')).toBeInTheDocument();
  });

  it('renders an optional action', () => {
    render(<EmptyState title="Nothing here" action={<button type="button">Find people</button>} />);
    expect(screen.getByRole('button', { name: 'Find people' })).toBeInTheDocument();
  });

  it('marks the icon decorative', () => {
    const { container } = render(<EmptyState icon={ImageIcon} title="Empty" />);
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });
});
