import {
  fetchViewerOrNull,
  requireAnonymous,
  requireOnboarded,
  requireOnboardedOrAnon,
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

export const rootLoader = asLoader(async () => ({ profile: await requireOnboarded() }));
export const viewerLoader = asLoader(async () => ({ viewer: await fetchViewerOrNull() }));

export const loginLoader = asLoader(requireAnonymous);
export const onboardingLoader = asLoader(requireOnboardedOrAnon);
