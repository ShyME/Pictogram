import { useMutation } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { Link, useLoaderData, useNavigate } from 'react-router';
import type { Profile } from './profile';
import { submitProfileEdit } from './profileApi';
import { USERNAME_MESSAGE, isUsernameShapeValid, usernameError } from './username';

export type EditProfileData = { profile: Profile };

type ServerError = 'username-shape' | 'username-taken' | 'details';

export function EditProfilePage() {
  const { profile } = useLoaderData() as EditProfileData;
  const navigate = useNavigate();
  const fieldId = useId();
  const usernameErrorId = useId();

  const [username, setUsername] = useState(profile.username);
  const [displayName, setDisplayName] = useState(profile.displayName ?? '');
  const [bio, setBio] = useState(profile.bio ?? '');
  const [serverError, setServerError] = useState<ServerError | null>(null);

  const shapeValid = isUsernameShapeValid(username);
  const isRenaming = username !== profile.username;
  const nameError = usernameError(username, serverError);

  const mutation = useMutation({
    mutationFn: submitProfileEdit,
    onSuccess: (outcome) => {
      switch (outcome.status) {
        case 'updated':
          navigate(`/u/${outcome.profile.username}`, { replace: true });
          break;
        case 'not-onboarded':
          navigate('/onboarding', { replace: true });
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
    if (!shapeValid) return;
    mutation.mutate({ username, displayName, bio });
  }

  return (
    <div className="min-h-dvh bg-neutral-50">
      <header className="border-b border-neutral-200 bg-white px-4 py-3">
        <Link to="/" className="font-semibold tracking-tight text-neutral-900">
          Pictogram
        </Link>
      </header>

      <main className="mx-auto max-w-xl px-4 py-8">
        <h1 className="text-xl font-semibold tracking-tight text-neutral-900">Edit profile</h1>

        <form
          onSubmit={(event) => {
            event.preventDefault();
            submit();
          }}
          className="mt-6 rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm"
        >
          <div>
            <label
              htmlFor={`${fieldId}-username`}
              className="block text-sm font-medium text-neutral-700"
            >
              Username
            </label>
            <input
              id={`${fieldId}-username`}
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
            {isRenaming && !nameError && (
              <p role="status" className="mt-1.5 text-sm text-amber-700">
                Changing your username breaks existing links. Anyone who visits your old{' '}
                <span className="font-medium">/u/{profile.username}</span> link will see a
                &ldquo;not found&rdquo; page.
              </p>
            )}
          </div>

          <div className="mt-4">
            <label
              htmlFor={`${fieldId}-display`}
              className="block text-sm font-medium text-neutral-700"
            >
              Display name <span className="font-normal text-neutral-400">(optional)</span>
            </label>
            <input
              id={`${fieldId}-display`}
              name="displayName"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              maxLength={50}
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-neutral-900 focus:outline-none"
            />
          </div>

          <div className="mt-4">
            <label
              htmlFor={`${fieldId}-bio`}
              className="block text-sm font-medium text-neutral-700"
            >
              Bio <span className="font-normal text-neutral-400">(optional)</span>
            </label>
            <textarea
              id={`${fieldId}-bio`}
              name="bio"
              value={bio}
              onChange={(event) => setBio(event.target.value)}
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

          <div className="mt-6 flex items-center gap-3">
            <button
              type="submit"
              disabled={!shapeValid || mutation.isPending}
              className="rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-neutral-700 disabled:opacity-50"
            >
              Save changes
            </button>
            <Link
              to={`/u/${profile.username}`}
              className="text-sm font-medium text-neutral-500 hover:text-neutral-900"
            >
              Cancel
            </Link>
          </div>
        </form>
      </main>
    </div>
  );
}
