package io.github.hankaviator.phoset;

import io.github.hankaviator.gappuccino.runtime.*;


/** LSPosed entry point, scoped exclusively to Google Photos. */
public final class PhoSetModule implements FeatureInstaller {
    private static final String PHOTOS_PACKAGE = "com.google.android.apps.photos";

    @Override
    public void install(TargetPackage loadPackageParam) {
        if (!PHOTOS_PACKAGE.equals(loadPackageParam.packageName)) {
            return;
        }

        GeoIntentFix.install();
        OutOfSyncUiReconciler.install();
        TrashConfirmationBypass.install(loadPackageParam.classLoader);
    }
}
