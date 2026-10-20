package io.github.hankaviator.gappuccino;

import io.github.libxposed.api.XposedModule;
import io.github.hankaviator.gappuccino.runtime.*;

public final class ModernModule extends XposedModule {
    private String processName;
    private String packageName;
    @Override public void onModuleLoaded(ModuleLoadedParam param) {
        HookRuntime.api = this;
        processName = param.getProcessName();
        HookRuntime.log("Modern API " + getApiVersion() + " / " + getFrameworkName() + " " + getFrameworkVersion());
    }
    @Override public void onPackageReady(PackageReadyParam param) {
        if (!param.isFirstPackage() || !TweakCatalog.isHookTarget(param.getPackageName())) return;
        packageName = param.getPackageName();
        try {
            if (!getRemotePreferences(FeatureSettings.FILE).getBoolean("_modern_ready", false)) {
                HookRuntime.log("Settings not published yet; open Gappuccino and restart this app"); return;
            }
            new HookEntry().install(new TargetPackage(param.getPackageName(), processName, param.getClassLoader(), param.getApplicationInfo()));
        } catch (Throwable error) { HookRuntime.log(error); }
    }
    @Override public boolean onHotReloading(HotReloadingParam param) {
        if (!HotReloadPolicy.supports(packageName, processName)) {
            HookRuntime.log("Hot reload deferred: app-specific hooks require an app restart");
            return false;
        }
        // Set state before retiring; a rejected state must leave current hooks active.
        param.setSavedInstanceState(new String[]{packageName, processName});
        if (!ProfilePromotionHook.prepareForReload()) {
            HookRuntime.log("Hot reload deferred: profile menu is active or UI thread is busy");
            return false;
        }
        HookRuntime.log("Hot reload prepared for " + packageName);
        return true;
    }
    @Override public void onHotReloaded(HotReloadedParam param) {
        HookRuntime.api = this;
        if (!(param.getSavedInstanceState() instanceof String[] state) || state.length != 2
                || !HotReloadPolicy.supports(state[0], state[1]) || !state[1].equals(param.getProcessName()))
            throw new IllegalStateException("Unsupported reload state; restart the app");
        packageName = state[0]; processName = state[1];
        if (!getRemotePreferences(FeatureSettings.FILE).getBoolean("_modern_ready", false))
            throw new IllegalStateException("Settings unavailable; restart the app");
        // Existing handles are owned by the framework, not transferred recipe instances.
        // Stable IDs atomically replace matching methods; unhook any leftover old methods.
        HookEntry.installProfilePromotions(packageName, processName);
        param.getOldHookHandles().forEach(handle -> {
            try { handle.unhook(); } catch (IllegalStateException alreadyReplaced) { }
        });
        HookRuntime.log("Hot reload complete for " + packageName + " / API " + getApiVersion());
    }
}
