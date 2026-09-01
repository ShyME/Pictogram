import { requireAnonymous, requireOnboarded, requireOnboardedOrAnon } from "./access-gate";

/**
 * The access gate throws `redirect(...)` on its reject paths. React Router
 * treats a thrown and a returned redirect alike, but callers that invoke a
 * loader directly expect the Response back — so unwrap a thrown Response here
 * and let anything else propagate.
 */
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

/** The post composer and the profile edit form both need the same onboarded profile the root layout does. */
export const newPostLoader = rootLoader;
export const editProfileLoader = rootLoader;

export const loginLoader = asLoader(requireAnonymous);
export const onboardingLoader = asLoader(requireOnboardedOrAnon);
