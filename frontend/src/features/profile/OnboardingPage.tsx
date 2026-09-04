import { Button, Card, Input, Textarea, toast } from '@shared';
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
          toast({ variant: 'success', title: "You're all set" });
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
    <main className="grid min-h-dvh place-items-center bg-canvas p-6">
      <Card className="w-full max-w-sm p-8">
        <form
          onSubmit={(event) => {
            event.preventDefault();
            submit();
          }}
        >
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">Pick a username</h1>
          <p className="mt-2 text-sm text-foreground-muted">
            This is how people find you. You can set a display name and bio too.
          </p>

          <div className="mt-6">
            <label htmlFor={usernameFieldId} className="block text-sm font-medium text-foreground">
              Username
            </label>
            <Input
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
              className="mt-1"
            />
            {nameError && (
              <p id={usernameErrorId} role="alert" className="mt-1.5 text-sm text-danger-text">
                {USERNAME_MESSAGE[nameError]}
              </p>
            )}
          </div>

          <div className="mt-4">
            <label
              htmlFor={`${usernameFieldId}-display`}
              className="block text-sm font-medium text-foreground"
            >
              Display name <span className="font-normal text-foreground-subtle">(optional)</span>
            </label>
            <Input
              id={`${usernameFieldId}-display`}
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
            <label
              htmlFor={`${usernameFieldId}-bio`}
              className="block text-sm font-medium text-foreground"
            >
              Bio <span className="font-normal text-foreground-subtle">(optional)</span>
            </label>
            <Textarea
              id={`${usernameFieldId}-bio`}
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

          <Button
            type="submit"
            disabled={!isShapeValid || mutation.isPending}
            className="mt-6 w-full"
          >
            Create profile
          </Button>
        </form>
      </Card>
    </main>
  );
}
