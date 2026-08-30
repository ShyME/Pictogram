# Profile

The public face of a user. Owns the username — the identity other people see and address
them by — and the onboarding step that first creates it.

## Language

**Profile**:
The public information about a user: username, display name, bio. Keyed by `UserId`. A user
has exactly one profile once onboarded.
_Avoid_: Account, page, bio (as the whole thing)

**Username**:
The unique, URL-safe handle for a user, matching `^[a-z0-9_]{3,20}$`. Used in API paths and
profile URLs. Changeable; in v1 a rename immediately frees the old handle and old links to it
break.
_Avoid_: Handle, slug, login, tag

**Display name**:
The free-text name shown on the profile and on feed cards (1–50 characters, may contain
spaces and emoji). Not unique. Never appears in a URL.
_Avoid_: Nickname, full name, screen name, alias

**Bio**:
An optional free-text description on the profile, up to 160 characters.
_Avoid_: About, description, status

**Onboarding**:
The step a person completes on first sign-in: choosing a username, which creates their
profile. Until it is done the person is a `User` (in identity) with no `Profile` — a valid
"not yet onboarded" state.
_Avoid_: Registration, sign-up, setup wizard
