package io.github.hankaviator.playstoreskipdisabled;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Parcelable;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Discover approval state from semantic Bundle keys and branch behavior. */
public final class Hook implements IXposedHookLoadPackage {
    private static final String ACTIVITY = "com.google.android.finsky.multiinstall.MultiInstallActivity";
    private static boolean installed;
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) {
        if (!"com.android.vending".equals(load.packageName)) return;
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (installed) return;
                installed = true;
                try { install((Context) param.args[0], load.classLoader); }
                catch (Throwable error) { log("Approval discovery unavailable; normal approval UI retained: " + error); }
            }
        });
    }

    private static void install(Context context, ClassLoader loader) throws Exception {
        ApprovalDiscovery.Mapping found = ApprovalDiscovery.resolve(new File(context.getApplicationInfo().sourceDir), ACTIVITY);
        Class<?> activity = Class.forName(ACTIVITY, false, loader);
        Class<?> approval = Class.forName(found.disabled().getDefiningClass()
                .substring(1, found.disabled().getDefiningClass().length() - 1).replace('/', '.'), false, loader);
        if (!Activity.class.isAssignableFrom(activity) || !Parcelable.class.isAssignableFrom(approval))
            throw new IllegalStateException("Unexpected approval host or item");
        Field pending = field(activity, found.pending().getName());
        Field index = field(activity, found.index().getName());
        Field disabled = field(approval, found.disabled().getName());
        if (!List.class.isAssignableFrom(pending.getType()) || index.getType() != int.class
                || disabled.getType() != boolean.class) throw new IllegalStateException("Unexpected approval field types");
        var renderer = activity.getDeclaredMethod(found.renderer(), boolean.class);
        if (renderer.getReturnType() != void.class) throw new IllegalStateException("Unexpected renderer");
        XposedBridge.hookMethod(renderer, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Activity host = (Activity) param.thisObject;
                    if (host.getIntent().getIntExtra("MultiInstallActivity.mode", 1) != 1) return;
                    List<?> items = (List<?>) pending.get(host);
                    if (items == null) return;
                    int original = index.getInt(host), next = original;
                    while (next >= 0 && next < items.size()) {
                        Object item = items.get(next);
                        if (!approval.isInstance(item) || !disabled.getBoolean(item)) break;
                        next++;
                    }
                    if (next != original) {
                        index.setInt(host, next);
                        log("Skipped " + (next - original) + " auto-update-disabled approval(s)");
                    }
                } catch (Throwable error) { log("Approval skip unavailable; normal UI retained: " + error); }
            }
        });
        log("Generic approval hook installed");
    }
    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static void log(String message) { XposedBridge.log("PlayStoreSkipDisabled: " + message); }
}
