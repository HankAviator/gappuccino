package io.github.hankaviator.gappuccino;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Explicit allowlist: selecting another package in LSPosed never hooks it. */
public final class HookEntry implements IXposedHookLoadPackage {
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) {
        if (!TweakCatalog.isHookTarget(load.packageName)) return;
        if (load.packageName.equals(load.processName) && !load.packageName.equals("com.google.android.as")) {
            boolean ai = enabled(TweakCatalog.PROFILE_AI);
            boolean points = load.packageName.equals("com.android.vending") && enabled("playstore_profile_points");
            if (ai || points) run("Profile promotions", () -> ProfilePromotionHook.install(ai, points));
        }
        switch (load.packageName) {
            case "com.google.android.apps.photos":
                run("Photos", () -> new io.github.hankaviator.phoset.PhoSetModule().handleLoadPackage(load));
                if (enabled("photos_tcp")) run("Photos TCP", () ->
                    io.github.hankaviator.dualpathvpn.PhotosTransportHook.install(load.classLoader));
                break;
            case "com.google.android.gm":
            case "com.google.android.apps.maps":
                run("Sponsored content", () -> new io.github.hankaviator.greatgadsbye.GreatGadsbye().handleLoadPackage(load));
                break;
            case "com.google.android.dialer":
                run("Phone", () -> new io.github.hankaviator.gdialertweak.HookEntry().handleLoadPackage(load));
                break;
            case "com.google.android.googlequicksearchbox":
                if (enabled("circle_native_menu")) run("Circle to Search", () ->
                    new io.github.hankaviator.ctsnativemenu.Hook().handleLoadPackage(load));
                break;
            case "com.google.android.as":
                if (enabled("asi_smart_reply")) run("Smart Reply locale", () -> SmartReplyHook.install(load.classLoader,
                        enabled("asi_smart_reply_locale"), enabled("asi_smart_reply_trace"), enabled("asi_telegram_capture")));
                if (enabled("asi_smart_reply_trace")) run("Smart Reply diagnostics", () -> SmartReplyTrace.install(load.classLoader));
                if (enabled("asi_multilingual")) run("Multilingual selection", () ->
                    new io.github.hankaviator.asimultilingual.Hook().handleLoadPackage(load));
                break;
            case "com.android.vending":
                if (enabled("playstore_skip_disabled")) run("Play Store approval", () ->
                    new io.github.hankaviator.playstoreskipdisabled.Hook().handleLoadPackage(load));
                if (enabled("playstore_self_update_lock")) run("Play Store self-update", () ->
                    new io.github.hankaviator.playstoreselfupdatelock.MainHook().handleLoadPackage(load));
                break;
        }
    }
    private static boolean enabled(String key) {
        try {
            XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, FeatureSettings.FILE);
            prefs.reload();
            if (key.equals(TweakCatalog.PROFILE_AI)) return TweakCatalog.profileAiEnabled(prefs.getAll());
            return prefs.getBoolean(key, TweakCatalog.defaultEnabled(key));
        } catch (Throwable error) {
            XposedBridge.log("Gappuccino: preferences unavailable for " + key + ": " + error);
            return TweakCatalog.defaultEnabled(key);
        }
    }
    private interface Install { void run() throws Throwable; }
    private static void run(String name, Install install) {
        try { install.run(); }
        catch (Throwable error) { XposedBridge.log("Gappuccino: " + name + " unavailable: " + error); }
    }
}
