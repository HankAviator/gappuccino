package io.github.hankaviator.gappuccino;
import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;
public class ProfilePromotionSettingsTest {
    @Test public void freshInstallDefaultsOn() { assertTrue(TweakCatalog.profileAiEnabled(Map.of())); }
    @Test public void priorOptOutMigratesOff() { assertFalse(TweakCatalog.profileAiEnabled(Map.of("profile_ai_com.google.android.gm", false))); }
    @Test public void sharedSettingOverridesLegacy() { assertTrue(TweakCatalog.profileAiEnabled(Map.of("profile_ai", true, "profile_ai_com.google.android.gm", false))); }
    @Test public void genericPageIsNotAnInjectionTarget() { assertFalse(TweakCatalog.isHookTarget(TweakCatalog.GENERIC)); assertTrue(TweakCatalog.isHookTarget("com.google.android.apps.docs")); assertFalse(TweakCatalog.isHookTarget("android")); }
}
