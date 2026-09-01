import { useSearchParams } from 'react-router';
import { ContinueWithGoogleButton } from './ContinueWithGoogleButton';
import { signInErrorMessage } from './sign-in-error';

export function LoginPage() {
  const [searchParams] = useSearchParams();
  const error = signInErrorMessage(searchParams.get('error'));

  return (
    <main className="grid min-h-dvh place-items-center bg-neutral-50 p-6">
      <div className="w-full max-w-sm rounded-2xl border border-neutral-200 bg-white p-8 text-center shadow-sm">
        <h1 className="text-2xl font-semibold tracking-tight text-neutral-900">Pictogram</h1>
        <p className="mt-2 text-sm text-neutral-500">Sign in to see your feed.</p>
        {error && (
          <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}
        <ContinueWithGoogleButton className="mt-6" />
      </div>
    </main>
  );
}
