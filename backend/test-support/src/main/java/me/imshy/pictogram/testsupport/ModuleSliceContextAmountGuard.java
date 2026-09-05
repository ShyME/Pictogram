package me.imshy.pictogram.testsupport;

/**
 * Measured floor per {@code @ApplicationModuleTest} module is 2 — the base
 * slice, plus the shared {@code @MockitoBean Clock} context that the
 * clock-controlled tests in {@code post} / {@code media} / {@code follow} share
 * (identity and profile stay at 1). Ceiling is floor + 1, matching
 * {@link ContextAmountGuard} usage elsewhere: one new deliberate context is
 * allowed, a second is drift.
 */
public final class ModuleSliceContextAmountGuard extends ContextAmountGuard {

    @Override
    protected int limit() {
        return 3;
    }

    @Override
    protected String scope() {
        return "This module slice";
    }
}
