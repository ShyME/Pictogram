# Identity

Authenticates people and issues the tokens the rest of Pictogram trusts. It knows nothing
about profiles, posts, or the social graph.

## Language

**User**:
An authenticated person, identified everywhere by a `UserId` (UUID). Identity stores only
what it needs to authenticate: the Google subject and the email Google reports.
_Avoid_: Account, member, principal

**Google subject**:
The stable, unique identifier Google assigns to a Google account (`sub`). The value
Pictogram authenticates against; it never changes for a given account.
_Avoid_: Google ID, external ID

**Identity provider**:
The external system that vouches for a person's identity. Google is the only one in v1;
the term exists because email/password is expected to become a second provider.
_Avoid_: Auth provider, IdP (in prose), broker

**Access token**:
A short-lived, Pictogram-issued token a client sends with each request. Signed by Pictogram
(EdDSA), never a Google token.
_Avoid_: Bearer token, JWT (when the audience matters), session

**Refresh token**:
A longer-lived, rotating, Pictogram-issued token used only to obtain a new access token.
_Avoid_: Session token

**Refresh token family**:
The rotation chain of refresh tokens that starts at one sign-in. Each use of a refresh
token consumes it and issues the next in the same family. Presenting a token that was
consumed longer than the rotation grace ago is treated as theft and revokes the whole
family — the person signs in again.
_Avoid_: Session, chain (in prose)

**Rotation grace**:
A short window (default 60s) after a refresh token is consumed during which presenting it
again is treated as a benign concurrent refresh — rejected with a 401, but the family is
not revoked. Absorbs accidental double-submits (retries, React StrictMode) without ending
the session.
_Avoid_: Grace period (that name is media's retention window)

**Session**:
The `(access token, refresh token)` pair a caller holds after signing in or refreshing.
Not a server-side object — Pictogram keeps no session state; the term names the pair. (The
Google sign-in handshake needs a short-lived servlet session to hold the authorization
request; it is invalidated the moment the Pictogram session is minted — ADR-0004 #27.)
_Avoid_: Login, ticket
