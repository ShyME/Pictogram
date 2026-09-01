const MESSAGES: Record<string, string> = {
  'email-unverified':
    "Google hasn't verified the email on that account. Verify it with Google, then try again.",
  'email-missing':
    "Google didn't share an email address for that account, so we can't sign you in.",
  'sign-in-failed': "Sign-in didn't complete. Please try again.",
};

export function signInErrorMessage(reason: string | null | undefined): string | null {
  if (!reason) return null;
  return Object.hasOwn(MESSAGES, reason) ? MESSAGES[reason] : MESSAGES['sign-in-failed'];
}
