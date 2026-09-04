import { Button, Card, Input, Textarea, toast } from '@shared';
import { useMutation } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { Link, useNavigate, useOutletContext } from 'react-router';
import type { Profile } from './profile';
import { submitProfileEdit } from './profileApi';
import { USERNAME_MESSAGE, isUsernameShapeValid, usernameError } from './username';

type ServerError = 'username-shape' | 'username-taken' | 'details';

export function EditProfilePage() {
  const { profile } = useOutletContext<{ profile: Profile }>();
  const navigate = useNavigate();
  const fieldId = useId();
  const usernameErrorId = useId();

  const [username, setUsername] = useState(profile.username);
  const [displayName, setDisplayName] = useState(profile.displayName ?? '');
  const [bio, setBio] = useState(profile.bio ?? '');
  const [serverError, setServerError] = useState<ServerError | null>(null);

  const isShapeValid = isUsernameShapeValid(username);
  const isRenaming = username !== profile.username;
  const nameError = usernameError(username, serverError);

  const mutation = useMutation({
    mutationFn: submitProfileEdit,
    onSuccess: (outcome) => {
      switch (outcome.status) {
        case 'updated':
          toast({ variant: 'success', title: 'Profile updated' });
          void navigate(`/u/${outcome.profile.username}`, { replace: true });
          break;
        case 'not-onboarded':
          void navigate('/onboarding', { replace: true });
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
    onError: () => {
      toast({ variant: 'error', title: 'Something went wrong', description: 'Please try again.' });
    },
  });

  function submit() {
    setServerError(null);
    if (!isShapeValid) return;
    mutation.mutate({ username, displayName, bio });
  }

  return (
    <main className="mx-auto max-w-xl px-4 py-8">
      <h1 className="text-xl font-semibold tracking-tight text-foreground">Edit profile</h1>

      <Card className="mt-6 p-6">
        <form
          onSubmit={(event) => {
            event.preventDefault();
            submit();
          }}
        >
          <div>
            <label
              htmlFor={`${fieldId}-username`}
              className="block text-sm font-medium text-foreground"
            >
              Username
            </label>
            <Input
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
              className="mt-1"
            />
            {nameError && (
              <p id={usernameErrorId} role="alert" className="mt-1.5 text-sm text-danger-text">
                {USERNAME_MESSAGE[nameError]}
              </p>
            )}
            {isRenaming && !nameError && (
              <p role="status" className="mt-1.5 break-words text-sm text-foreground-muted">
                Changing your username breaks existing links. Anyone who visits your old{' '}
                <span className="font-medium text-foreground">/u/{profile.username}</span> link will
                see a &ldquo;not found&rdquo; page.
              </p>
            )}
          </div>

          <div className="mt-4">
            <label
              htmlFor={`${fieldId}-display`}
              className="block text-sm font-medium text-foreground"
            >
              Display name <span className="font-normal text-foreground-subtle">(optional)</span>
            </label>
            <Input
              id={`${fieldId}-display`}
              name="displayName"
              value={displayName}
              onChange={(event) => {
                setDisplayName(event.target.value);
              }}
              maxLength={50}
              className="mt-1"
            />
          </div>

          <div className="mt-4">
            <label htmlFor={`${fieldId}-bio`} className="block text-sm font-medium text-foreground">
              Bio <span className="font-normal text-foreground-subtle">(optional)</span>
            </label>
            <Textarea
              id={`${fieldId}-bio`}
              name="bio"
              value={bio}
              onChange={(event) => {
                setBio(event.target.value);
              }}
              maxLength={160}
              rows={3}
              className="mt-1 resize-none"
            />
          </div>

          {serverError === 'details' && (
            <p role="alert" className="mt-4 text-sm text-danger-text">
              Your display name or bio is too long. Shorten it and try again.
            </p>
          )}

          <div className="mt-6 flex items-center gap-3">
            <Button type="submit" disabled={!isShapeValid || mutation.isPending}>
              Save changes
            </Button>
            <Button variant="ghost" asChild>
              <Link to={`/u/${profile.username}`}>Cancel</Link>
            </Button>
          </div>
        </form>
      </Card>
    </main>
  );
}
