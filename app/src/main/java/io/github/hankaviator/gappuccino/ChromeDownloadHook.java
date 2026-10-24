package io.github.hankaviator.gappuccino;

import android.app.Application;
import android.content.Context;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import io.github.hankaviator.gappuccino.runtime.*;

final class ChromeDownloadHook {
    private static final String BRIDGE = "org.chromium.chrome.browser.download.DangerousDownloadDialogBridge";
    private static boolean attempted;
    static void install() {
        Reflection.findAndHookMethod(Application.class, "attach", Context.class, new Callback() {
            @Override protected void afterHookedMethod(MethodHookParam call) {
                Context context = (Context) call.args[0];
                try {
                    ClassLoader loader = context.getClassLoader();
                    Class<?> bridge = Reflection.findClassIfExists(BRIDGE, loader);
                    if (bridge == null) {
                        loader = context.createContextForSplit("chrome").getClassLoader();
                        bridge = Class.forName(BRIDGE, false, loader);
                    }
                    ClassLoader chromeLoader = loader;
                    HookRuntime.hookAllMethods(bridge, "showDialog", new Callback() {
                        @Override protected void beforeHookedMethod(MethodHookParam call) {
                            prepare(context, chromeLoader);
                        }
                    });
                    HookRuntime.log("Gappuccino: Chrome download confirmation trigger installed");
                } catch (Throwable error) { HookRuntime.log("Gappuccino: Chrome download confirmation unavailable: " + error); }
            }
        });
    }
    private static synchronized void prepare(Context context, ClassLoader loader) {
        if (attempted) return;
        attempted = true;
        try {
            List<File> apks = new ArrayList<>();
            var info = context.getApplicationInfo();
            apks.add(new File(info.sourceDir));
            if (info.splitSourceDirs != null) for (String split : info.splitSourceDirs) apks.add(new File(split));
            var mapping = ChromeDownloadDiscovery.resolve(apks);
            Class<?> owner = Class.forName(className(mapping.owner()), false, loader);
            Class<?>[] params = new Class<?>[mapping.parameters().size()];
            for (int i = 0; i < params.length; i++) {
                String p = mapping.parameters().get(i);
                params[i] = switch (p) { case "I" -> int.class; case "J" -> long.class; case "Z" -> boolean.class;
                    default -> Class.forName(className(p), false, loader); };
            }
            Method show = owner.getDeclaredMethod(mapping.method(), params);
            Method result = params[mapping.callback()].getMethod("onResult", Object.class);
            HookRuntime.hookMethod(show, new Callback() {
                @Override protected void beforeHookedMethod(MethodHookParam call) throws Throwable {
                    if (mapping.dangerous() >= 0 && !Boolean.TRUE.equals(call.args[mapping.dangerous()])) return;
                    if (!new HookPreferences(BuildConfig.APPLICATION_ID, FeatureSettings.FILE).getBoolean("chrome_skip_download_confirmation", true)) return;
                    Object callback = call.args[mapping.callback()];
                    if (callback == null) return;
                    // Mark handled before delivering Chrome's callback: never invoke it twice
                    // if its own completion code throws after accepting the download.
                    call.setResult(null);
                    try { result.invoke(callback, ChromeDownloadDiscovery.accepted(mapping.result())); }
                    catch (InvocationTargetException error) { HookRuntime.log(error.getCause()); }
                    HookRuntime.log("Gappuccino: accepted Chrome file-type download confirmation");
                }
            });
            HookRuntime.log("Gappuccino: Chrome download confirmation discovered (" + mapping.result() + ")");
        } catch (Throwable error) { HookRuntime.log("Gappuccino: Chrome download discovery unavailable; original confirmation retained: " + error); }
    }
    private static String className(String descriptor) { return descriptor.substring(1, descriptor.length()-1).replace('/', '.'); }
    private ChromeDownloadHook() {}
}
