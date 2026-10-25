package io.github.hankaviator.gappuccino;

import io.github.libxposed.api.XposedModule;
import io.github.hankaviator.gappuccino.runtime.*;

public final class ModernModule extends XposedModule {
    private String processName;
    @Override public void onModuleLoaded(ModuleLoadedParam param) {
        HookRuntime.api = this;
        processName = param.getProcessName();
        HookRuntime.log("Modern API " + getApiVersion() + " / " + getFrameworkName() + " " + getFrameworkVersion());
    }
    @Override public void onPackageReady(PackageReadyParam param) {
        if (!param.isFirstPackage() || !TweakCatalog.isHookTarget(param.getPackageName())) return;
        try {
            if (!getRemotePreferences(FeatureSettings.FILE).getBoolean("_modern_ready", false)) {
                HookRuntime.log("Settings not published yet; open Gappuccino and restart this app"); return;
            }
            new HookEntry().install(new TargetPackage(param.getPackageName(), processName, param.getClassLoader(), param.getApplicationInfo()));
        } catch (Throwable error) { HookRuntime.log(error); }
    }
    @Override public boolean onHotReloading(HotReloadingParam param) {
        // Reject manual requests too; APK updates require restarting target apps.
        return false;
    }
}
