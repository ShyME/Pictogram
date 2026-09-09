import type { Page } from '@playwright/test';
import { expect, test } from './fixtures';
import { ChatRailPage } from './pages/chatRail.page';
import { FeedPage } from './pages/feed.page';
import { LoginPage } from './pages/login.page';
import { OnboardingPage } from './pages/onboarding.page';
import { ProfilePage } from './pages/profile.page';

// #203 end-to-end, spanning app + chat: sign in → the rail lists who the viewer follows,
// with live presence → click a row → the overlay opens → send a message the peer receives.
test('the chat rail lists followed users with presence and opens a conversation', async ({
  browser,
}) => {
  const suffix = Date.now().toString(36);
  const asker = `e2e_rail_a_${suffix}`;
  const peer = `e2e_rail_b_${suffix}`;

  const askerContext = await browser.newContext();
  const peerContext = await browser.newContext();

  try {
    const askerPage = await askerContext.newPage();
    await onboard(askerPage, asker, 'Rail Asker');

    const peerPage = await peerContext.newPage();
    await onboard(peerPage, peer, 'Rail Peer');

    // The asker follows the peer, so the peer is the row the rail shows.
    const askerViewOfPeer = new ProfilePage(askerPage);
    await askerViewOfPeer.open(peer);
    await askerViewOfPeer.followButton.click();
    await expect(askerViewOfPeer.followingButton).toBeVisible();

    // The peer stays on a screen, so their session socket is connected and presence reads online.
    await new FeedPage(peerPage).open();

    await askerPage.goto('/');
    const rail = new ChatRailPage(askerPage);
    await expect(rail.row('Rail Peer')).toBeVisible();
    await expect(rail.presenceDot('Online')).toBeVisible();

    await rail.row('Rail Peer').click();
    const overlay = rail.overlay('Rail Peer');
    await expect(overlay).toBeVisible();

    await overlay.getByRole('textbox').fill('first message from the rail');
    await overlay.getByRole('button', { name: 'Send' }).click();
    await expect(overlay.getByText('first message from the rail')).toBeVisible();
    await expect(overlay.getByText('Not delivered')).toBeHidden();

    // The peer receives it — a toast, since their overlay is not open on this conversation.
    await expect(peerPage.getByText(/new message from rail asker/i)).toBeVisible();
    await expect(peerPage.getByText('first message from the rail')).toBeVisible();
  } finally {
    await askerContext.close();
    await peerContext.close();
  }
});

async function onboard(page: Page, username: string, displayName: string): Promise<void> {
  const login = new LoginPage(page);
  const onboarding = new OnboardingPage(page);
  const feed = new FeedPage(page);

  await login.open();
  await login.signInWithGoogle();
  await expect(page).toHaveURL(/\/onboarding$/);
  await onboarding.completeWith(username, displayName);
  await expect(feed.emptyState).toBeVisible();
}
