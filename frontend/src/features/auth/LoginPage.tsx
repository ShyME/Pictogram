import { PictogramMark } from '@shared';
import { useSearchParams } from 'react-router';
import { ContinueWithGoogleButton } from './ContinueWithGoogleButton';
import { signInErrorMessage } from './signInError';

export function LoginPage() {
  const [searchParams] = useSearchParams();
  const error = signInErrorMessage(searchParams.get('error'));

  return (
    <main className="grid min-h-dvh place-items-center bg-canvas p-6">
      <div className="w-full max-w-sm rounded-dialog border border-border bg-surface p-8 text-center shadow-card">
        <PictogramMark className="mx-auto size-10 text-foreground" />
        <h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground">Pictogram</h1>
        <p className="mt-2 text-sm text-foreground-muted">Sign in to see your feed.</p>
        {error && (
          <p
            role="alert"
            className="mt-4 rounded-control border border-danger-border bg-danger-surface px-3 py-2 text-sm text-danger-text"
          >
            {error}
          </p>
        )}
        <ContinueWithGoogleButton className="mt-6" />
      </div>
    </main>
  );
}
