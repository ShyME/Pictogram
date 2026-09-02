import { useMutation } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { useNavigate } from 'react-router';
import { submitOnboarding } from './profileApi';
import { USERNAME_MESSAGE, isUsernameShapeValid, usernameError } from './username';

type ServerError = 'username-shape' | 'username-taken' | 'details';

export function OnboardingPage() {
  const navigate = useNavigate();
  const usernameFieldId = useId();
  const usernameErrorId = useId();

  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [serverError, setServerError] = useState<ServerError | null>(null);

  const isShapeValid = isUsernameShapeValid(username);
  const nameError = usernameError(username, serverError);

  const mutation = useMutation({
    mutationFn: submitOnboarding,
    onSuccess: (outcome) => {
      switch (outcome.status) {
        case 'created':
        case 'already-onboarded':
          void navigate('/', { replace: true });
          break;
        case 'username-taken':
          setServerError('username-taken');
          break;
        case 'username-invalid':
          setServerError('username-shape');
          break;
        case 'details-invalid':
          setServerError('details');
          break;
      }
    },
  });

  function submit() {
    setServerError(null);
    if (!isShapeValid) return;
    mutation.mutate({ username, displayName, bio });
  }

  return (
    <main className="grid min-h-dvh place-items-center bg-neutral-50 p-6">
      <form
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
        className="w-full max-w-sm rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm"
      >
        <h1 className="text-2xl font-semibold tracking-tight text-neutral-900">Pick a username</h1>
        <p className="mt-2 text-sm text-neutral-500">
          This is how people find you. You can set a display name and bio too.
        </p>

        <div className="mt-6">
          <label htmlFor={usernameFieldId} className="block text-sm font-medium text-neutral-700">
            Username
          </label>
          <input
            id={usernameFieldId}
            name="username"
            value={username}
            onChange={(event) => {
              setUsername(event.target.value);
              if (serverError) setServerError(null);
            }}
            autoComplete="off"
            autoCapitalize="none"
            spellCheck={false}
            required
            aria-invalid={nameError !== null}
            aria-describedby={nameError ? usernameErrorId : undefined}
            className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-neutral-900 focus:outline-none aria-[invalid=true]:border-red-500"
          />
          {nameError && (
            <p id={usernameErrorId} role="alert" className="mt-1.5 text-sm text-red-600">
              {USERNAME_MESSAGE[nameError]}
            </p>
          )}
        </div>

        <div className="mt-4">
          <label
            htmlFor={`${usernameFieldId}-display`}
            className="block text-sm font-medium text-neutral-700"
          >
            Display name <span className="font-normal text-neutral-400">(optional)</span>
          </label>
          <input
            id={`${usernameFieldId}-display`}
            name="displayName"
            value={displayName}
            onChange={(event) => {
              setDisplayName(event.target.value);
            }}
            maxLength={50}
            className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-neutral-900 focus:outline-none"
          />
        </div>

        <div className="mt-4">
          <label
            htmlFor={`${usernameFieldId}-bio`}
            className="block text-sm font-medium text-neutral-700"
          >
            Bio <span className="font-normal text-neutral-400">(optional)</span>
          </label>
          <textarea
            id={`${usernameFieldId}-bio`}
            name="bio"
            value={bio}
            onChange={(event) => {
              setBio(event.target.value);
            }}
            maxLength={160}
            rows={3}
            className="mt-1 w-full resize-none rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-neutral-900 focus:outline-none"
          />
        </div>

        {serverError === 'details' && (
          <p role="alert" className="mt-4 text-sm text-red-600">
            Your display name or bio is too long. Shorten it and try again.
          </p>
        )}
        {mutation.isError && (
          <p role="alert" className="mt-4 text-sm text-red-600">
            Something went wrong. Please try again.
          </p>
        )}

        <button
          type="submit"
          disabled={!isShapeValid || mutation.isPending}
          className="mt-6 w-full rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-neutral-700 disabled:opacity-50"
        >
          Create profile
        </button>
      </form>
    </main>
  );
}
