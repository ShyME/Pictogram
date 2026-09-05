package me.imshy.pictogram;

import me.imshy.pictogram.testsupport.ContextAmountGuard;

public final class AppContextAmountGuard extends ContextAmountGuard {

    @Override
    protected int limit() {
        return 9;
    }

    @Override
    protected String scope() {
        return "The app suite";
    }
}
