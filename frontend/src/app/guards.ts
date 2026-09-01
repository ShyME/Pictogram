import { requireAnonymous, requireOnboarded, requireOnboardedOrAnon } from "./access-gate";

const asLoader =
  <T>(load: () => Promise<T>) =>
  async (): Promise<T | Response> => {
    try {
      return await load();
    } catch (thrown) {
      if (thrown instanceof Response) return thrown;
      throw thrown;
    }
  };

export const rootLoader = asLoader(async () => ({ profile: await requireOnboarded() }));

export const newPostLoader = rootLoader;
export const editProfileLoader = rootLoader;

export const loginLoader = asLoader(requireAnonymous);
export const onboardingLoader = asLoader(requireOnboardedOrAnon);
