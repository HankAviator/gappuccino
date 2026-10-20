package io.github.hankaviator.gappuccino;

import io.github.hankaviator.gappuccino.runtime.*;


/** Explicit allowlist: selecting another package in LSPosed never hooks it. */
public final class HookEntry implements FeatureInstaller {
    @Override public void install(TargetPackage load) {
        if (!TweakCatalog.isHookTarget(load.packageName)) return;
        run("Profile promotions", () -> installProfilePromotions(load.packageName, load.processName));
        switch (load.packageName) {
            case "com.google.android.keep":
                if (load.packageName.equals(load.processName) && enabled("keep_skip_delete_confirmation")) run("Keep confirmation", KeepDeleteHook::install);
                break;
            case "com.google.android.apps.fitness":
                if (load.packageName.equals(load.processName) && enabled("fit_hide_period")) run("Fit period UI", FitPeriodHook::install);
                break;
            case "com.google.android.apps.photos":
                run("Photos", () -> new io.github.hankaviator.phoset.PhoSetModule().install(load));
                if (enabled("photos_tcp")) run("Photos TCP", () ->
                    io.github.hankaviator.dualpathvpn.PhotosTransportHook.install(load.classLoader));
                break;
            case "com.google.android.gm":
            case "com.google.android.apps.maps":
                run("Sponsored content", () -> new io.github.hankaviator.greatgadsbye.GreatGadsbye().install(load));
                break;
            case "com.google.android.dialer":
                run("Phone", () -> new io.github.hankaviator.gdialertweak.HookEntry().install(load));
                break;
            case "com.google.android.googlequicksearchbox":
                if (enabled("circle_native_menu")) run("Circle to Search", () ->
                    new io.github.hankaviator.ctsnativemenu.Hook().install(load));
                break;
            case "com.android.vending":
                if (enabled("playstore_skip_disabled")) run("Play Store approval", () ->
                    new io.github.hankaviator.playstoreskipdisabled.Hook().install(load));
                if (enabled("playstore_self_update_lock")) run("Play Store self-update", () ->
                    new io.github.hankaviator.playstoreselfupdatelock.MainHook().install(load));
                break;
        }
    }
    static void installProfilePromotions(String pkg, String process) {
        if (!pkg.equals(process) || !TweakCatalog.PROFILE_PACKAGES.contains(pkg)) return;
        boolean ai = enabled(TweakCatalog.PROFILE_AI);
        boolean points = pkg.equals("com.android.vending") && enabled("playstore_profile_points");
        if (ai || points) ProfilePromotionHook.install(ai, points);
    }
    private static boolean enabled(String key) {
        try {
            HookPreferences prefs = new HookPreferences(BuildConfig.APPLICATION_ID, FeatureSettings.FILE);
            prefs.reload();
            if (key.equals(TweakCatalog.PROFILE_AI)) return TweakCatalog.profileAiEnabled(prefs.getAll());
            return prefs.getBoolean(key, TweakCatalog.defaultEnabled(key));
        } catch (Throwable error) {
            HookRuntime.log("Gappuccino: preferences unavailable for " + key + ": " + error);
            return TweakCatalog.defaultEnabled(key);
        }
    }
    private interface Install { void run() throws Throwable; }
    private static void run(String name, Install install) {
        try { install.run(); }
        catch (Throwable error) { HookRuntime.log("Gappuccino: " + name + " unavailable: " + error); }
    }
}
