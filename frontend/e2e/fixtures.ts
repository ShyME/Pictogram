import { test as base, expect } from '@playwright/test';

export { expect } from '@playwright/test';

type Fixtures = {
  cspViolations: string[];
};

// Auto fixture: any screen a spec visits that reports a CSP violation fails the test. A
// React `style={}` prop is not one — style-src does not govern it (see ADR-0011 decision 3).
export const test = base.extend<Fixtures>({
  cspViolations: [
    async ({ page }, use) => {
      const violations: string[] = [];
      page.on('console', (message) => {
        if (message.type() === 'error' && /content security policy/i.test(message.text())) {
          violations.push(message.text());
        }
      });
      await use(violations);
      expect(violations).toEqual([]);
    },
    { auto: true },
  ],
});
