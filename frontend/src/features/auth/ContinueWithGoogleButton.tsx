import { Button, cn } from '@shared';

export const GOOGLE_SIGN_IN_PATH = '/oauth2/authorization/google';

export function ContinueWithGoogleButton({ className = '' }: { className?: string }) {
  return (
    <Button asChild size="lg" className={cn('w-full', className)}>
      <a href={GOOGLE_SIGN_IN_PATH}>Continue with Google</a>
    </Button>
  );
}
