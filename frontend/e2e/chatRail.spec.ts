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
    // `exact` so the match is the toast body, not Radix's concatenated live-region string.
    await expect(peerPage.getByText('New message from Rail Asker', { exact: true })).toBeVisible();
    await expect(peerPage.getByText('first message from the rail', { exact: true })).toBeVisible();
  } finally {
    await askerContext.close();
    await peerContext.close();
  }
});

// #204 end-to-end, spanning app + chat, at a narrow viewport where the rail is a drawer:
// a message from a conversation the viewer doesn't have open marks that person's rail row
// unread and badges the `AppNav` chat icon (alongside the toast); opening the row clears
// both.
test.describe('unread markers (narrow viewport)', () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test('a message from a non-open conversation marks the rail row and badges the nav icon', async ({
    browser,
  }) => {
    // Usernames cap at 20 characters, so the prefix has to stay short (see onboarding).
    const suffix = Date.now().toString(36);
    const sender = `e2e_un_s_${suffix}`;
    const viewer = `e2e_un_v_${suffix}`;
    const other = `e2e_un_o_${suffix}`;

    const senderContext = await browser.newContext();
    const viewerContext = await browser.newContext();
    const otherContext = await browser.newContext();

    try {
      const senderPage = await senderContext.newPage();
      await onboard(senderPage, sender, 'Unread Sender');

      const viewerPage = await viewerContext.newPage();
      await onboard(viewerPage, viewer, 'Unread Viewer');

      const otherPage = await otherContext.newPage();
      await onboard(otherPage, other, 'Unread Other');

      // The viewer follows both, so both are rows on the viewer's rail.
      for (const target of [sender, other]) {
        const profile = new ProfilePage(viewerPage);
        await profile.open(target);
        await profile.followButton.click();
        await expect(profile.followingButton).toBeVisible();
      }

      // The other user stays on a screen so their session socket is connected.
      await new FeedPage(otherPage).open();

      // The viewer opens a conversation with the other user — not the sender.
      await viewerPage.goto('/');
      const rail = new ChatRailPage(viewerPage);
      await rail.openDrawer();
      await rail.row('Unread Other').click();
      await expect(rail.overlay('Unread Other')).toBeVisible();
      await expect(rail.navUnreadBadge).toBeHidden();

      // The sender messages the viewer from the viewer's profile.
      const senderViewOfViewer = new ProfilePage(senderPage);
      await senderViewOfViewer.open(viewer);
      await senderPage.getByRole('button', { name: /^message$/i }).click();
      const composed = senderPage.getByRole('dialog', { name: 'Chat with Unread Viewer' });
      await composed.getByRole('textbox').fill('ping while you were away');
      await composed.getByRole('button', { name: 'Send' }).click();

      // The viewer gets the toast and the badged nav icon. `exact` so the match is the
      // toast body, not Radix's concatenated live-region string.
      await expect(
        viewerPage.getByText('New message from Unread Sender', { exact: true }),
      ).toBeVisible();
      await expect(rail.navUnreadBadge).toBeVisible();

      // The drawer marks the sender's row unread — not the other's.
      await rail.openDrawer();
      await expect(rail.unreadMarker('Unread Sender')).toBeVisible();
      await expect(rail.unreadMarker('Unread Other')).toBeHidden();

      // Opening the sender's row clears both the row marker and the nav badge.
      await rail.row('Unread Sender').click();
      await expect(rail.overlay('Unread Sender')).toBeVisible();
      await expect(rail.navUnreadBadge).toBeHidden();
      await rail.openDrawer();
      await expect(rail.unreadMarker('Unread Sender')).toBeHidden();
    } finally {
      await senderContext.close();
      await viewerContext.close();
      await otherContext.close();
    }
  });
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
