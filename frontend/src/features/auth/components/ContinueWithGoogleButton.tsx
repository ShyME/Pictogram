// A full-page navigation, not a client-side route change: it hands off to the backend's
// OIDC entry point, which redirects to Google and back (ADR-0004).
export const GOOGLE_SIGN_IN_PATH = "/oauth2/authorization/google";

export function ContinueWithGoogleButton({ className = "" }: { className?: string }) {
  return (
    <a
      href={GOOGLE_SIGN_IN_PATH}
      className={`inline-flex w-full items-center justify-center rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-neutral-700 ${className}`}
    >
      Continue with Google
    </a>
  );
}
