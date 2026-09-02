package me.imshy.pictogram.testsupport;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Fails the build the moment a test boots more distinct {@link ApplicationContext}s than the
 * measured floor allows. This is the permanent form of the #78 benchmark: context hygiene can only
 * be kept honest by a check that runs every build. Subclasses bind it to one test group and its
 * measured ceiling; each concrete guard counts its own contexts (each Gradle module runs its tests
 * in a fresh JVM, so the tallies never cross).
 */
public abstract class DistinctContextGuard implements BeforeEachCallback {

    private static final Map<Class<?>, Set<ApplicationContext>> SEEN = new ConcurrentHashMap<>();

    protected abstract int limit();

    protected abstract String scope();

    @Override
    public void beforeEach(ExtensionContext context) {
        Set<ApplicationContext> seen = SEEN.computeIfAbsent(
                getClass(), key -> Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>())));
        seen.add(SpringExtension.getApplicationContext(context));
        if (seen.size() > limit()) {
            throw new AssertionError(
                    "%s booted %d distinct ApplicationContexts (ceiling %d) — a test drifted its context configuration. See #78."
                            .formatted(scope(), seen.size(), limit()));
        }
    }
}
