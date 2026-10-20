package io.github.hankaviator.gappuccino;

import org.junit.Test;
import static org.junit.Assert.*;

public class HotReloadPolicyTest {
    @Test public void genericOnlyMainProcessesCanReload() {
        for (String pkg : new String[]{"com.google.android.calendar", "com.google.android.apps.docs",
                "com.google.android.apps.bard", "com.google.android.youtube", "com.android.chrome"})
            assertTrue(pkg, HotReloadPolicy.supports(pkg, pkg));
    }
    @Test public void everyAppSpecificRecipeRetainsRestartRequirement() {
        for (TweakCatalog.App app : TweakCatalog.APPS)
            assertFalse(app.packageName(), HotReloadPolicy.supports(app.packageName(), app.packageName()));
    }
    @Test public void noSystemUnknownAuxiliaryOrUninitializedProcess() {
        assertFalse(TweakCatalog.isHookTarget("com.google.android.as"));
        assertNull(TweakCatalog.find("com.google.android.as"));
        assertFalse(HotReloadPolicy.supports("android", "android"));
        assertFalse(HotReloadPolicy.supports("com.google.android.unknown", "com.google.android.unknown"));
        assertFalse(HotReloadPolicy.supports("com.google.android.calendar", "com.google.android.calendar:remote"));
        assertFalse(HotReloadPolicy.supports(null, "com.google.android.calendar"));
        assertFalse(HotReloadPolicy.supports("com.google.android.calendar", null));
    }
}
