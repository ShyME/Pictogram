import { Avatar, AvatarFallback, AvatarImage } from '@shared';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

describe('Avatar', () => {
  it('shows the fallback while the image has not loaded', async () => {
    render(
      <Avatar size="lg">
        <AvatarImage src="https://example.test/a.jpg" alt="Ansel" />
        <AvatarFallback>AA</AvatarFallback>
      </Avatar>,
    );
    expect(await screen.findByText('AA')).toBeInTheDocument();
  });

  it('applies the size variant to the root', () => {
    const { container } = render(
      <Avatar size="sm">
        <AvatarFallback>AA</AvatarFallback>
      </Avatar>,
    );
    expect(container.firstElementChild).toHaveClass('size-7');
  });
});
