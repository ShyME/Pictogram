import { CAPTION_MAX_LENGTH, captionLength, isCaptionWithinLimit } from '@features/post/caption';
import { expect, test } from 'vitest';

test('captionLength trims and counts code points, so an emoji is one', () => {
  expect(captionLength('  hello  ')).toBe(5);
  expect(captionLength('📷📷📷')).toBe(3);
});

test('isCaptionWithinLimit is true up to the limit and false past it', () => {
  expect(isCaptionWithinLimit('x'.repeat(CAPTION_MAX_LENGTH))).toBe(true);
  expect(isCaptionWithinLimit('x'.repeat(CAPTION_MAX_LENGTH + 1))).toBe(false);
  expect(isCaptionWithinLimit('')).toBe(true);
});
