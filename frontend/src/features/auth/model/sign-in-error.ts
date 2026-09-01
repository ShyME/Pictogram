// The `?error=` reason the backend puts on its sign-in-error redirect when Google sign-in
// can't complete (ADR-0004 #27). An absent reason shows no message; a present but
// unrecognised one falls back to the generic failure copy.
const MESSAGES: Record<string, string> = {
  "email-unverified":
    "Google hasn't verified the email on that account. Verify it with Google, then try again.",
  "email-missing": "Google didn't share an email address for that account, so we can't sign you in.",
  "sign-in-failed": "Sign-in didn't complete. Please try again.",
};

export function signInErrorMessage(reason: string | null | undefined): string | null {
  if (!reason) return null;
  // Plain-object lookup on user-controlled input would resolve `__proto__` / `constructor`
  // to prototype members, so only own string keys count.
  return Object.hasOwn(MESSAGES, reason) ? MESSAGES[reason] : MESSAGES["sign-in-failed"];
}
