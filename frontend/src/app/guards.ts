import type { Profile } from '@features/profile';
import type { LoaderFunctionArgs } from 'react-router';
import {
  fetchViewerOrNull,
  requireAnonymous,
  requireOnboarded,
  requireOnboardedOrAnon,
  requireOnboardedOrPublic,
} from './accessGate';

const asLoader =
  <T>(load: () => Promise<T>) =>
  async (): Promise<T | Response> => {
    try {
      return await load();
    } catch (error) {
      if (error instanceof Response) return error;
      throw error;
    }
  };

export async function rootLoader({
  request,
}: LoaderFunctionArgs): Promise<Response | { profile: Profile }> {
  try {
    const isHome = new URL(request.url).pathname === '/';
    const profile = await (isHome ? requireOnboardedOrPublic() : requireOnboarded());
    return { profile };
  } catch (error) {
    if (error instanceof Response) return error;
    throw error;
  }
}

export const viewerLoader = asLoader(async () => ({ viewer: await fetchViewerOrNull() }));

export const loginLoader = asLoader(requireAnonymous);
export const onboardingLoader = asLoader(requireOnboardedOrAnon);
