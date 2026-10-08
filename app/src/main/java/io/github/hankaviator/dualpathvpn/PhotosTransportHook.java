package io.github.hankaviator.dualpathvpn;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Lets Photos' Cronet traffic reach HyperOS's TCP-only SLA routing rules. */
public final class PhotosTransportHook {
    private final ClassLoader loader;
    private final ArrayList<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    private final AtomicBoolean configuredLogged = new AtomicBoolean();
    private volatile boolean enabled;
    private ContentObserver observer;

    private PhotosTransportHook(ClassLoader loader) {
        this.loader = loader;
    }

    public static void install(ClassLoader loader) {
        PhotosTransportHook transport = new PhotosTransportHook(loader);
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class,
                new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                transport.observe((Context) param.args[0]);
            }
        });
    }

    private void observe(Context context) {
        if (observer != null) {
            return;
        }
        ContentResolver resolver = context.getContentResolver();
        observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                update(resolver);
            }
        };
        try {
            resolver.registerContentObserver(Settings.System.getUriFor("linkturbo_is_enable"),
                    false, observer);
            update(resolver);
        } catch (RuntimeException error) {
            log("cannot observe boost switch: " + error);
        }
    }

    private synchronized void update(ContentResolver resolver) {
        boolean boost;
        try {
            boost = Settings.System.getInt(resolver, "linkturbo_is_enable", 0) == 1;
        } catch (RuntimeException error) {
            boost = false;
            log("cannot read boost switch: " + error);
        }
        enabled = boost;
        if (!boost) {
            for (XC_MethodHook.Unhook hook : hooks) {
                hook.unhook();
            }
            hooks.clear();
            configuredLogged.set(false);
            log("TCP compatibility hooks disabled");
            return;
        }
        if (!hooks.isEmpty()) {
            return;
        }
        try {
            Class<?> builder = XposedHelpers.findClass("org.chromium.net.CronetEngine$Builder",
                    loader);
            XC_MethodHook configure = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    configureTcp(param.thisObject);
                }
            };
            hooks.addAll(XposedBridge.hookAllConstructors(builder, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    configureTcp(param.thisObject);
                }
            }));
            hooks.addAll(XposedBridge.hookAllMethods(builder, "buildExperimental", configure));
            // Cover both public builders, including overridden/bridge methods.
            hookBuilderMethods(builder, configure);
            Class<?> experimental = XposedHelpers.findClassIfExists(
                    "org.chromium.net.ExperimentalCronetEngine$Builder", loader);
            if (experimental != null) {
                hookBuilderMethods(experimental, configure);
            }
            log("TCP compatibility hooks enabled");
        } catch (Throwable error) {
            enabled = false;
            for (XC_MethodHook.Unhook hook : hooks) {
                hook.unhook();
            }
            hooks.clear();
            log("Cronet hooks unavailable: " + error);
        }
    }

    private void hookBuilderMethods(Class<?> builder, XC_MethodHook configure) {
        hooks.addAll(XposedBridge.hookAllMethods(builder, "build", configure));
        hooks.addAll(XposedBridge.hookAllMethods(builder, "enableQuic", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (enabled) {
                    param.args[0] = false;
                }
            }
        }));
    }

    private void configureTcp(Object builder) {
        if (!enabled) {
            return;
        }
        try {
            Object delegate = XposedHelpers.getObjectField(builder, "mBuilderDelegate");
            XposedHelpers.callMethod(delegate, "enableQuic", false);
            if (configuredLogged.compareAndSet(false, true)) {
                log("configured Cronet with QUIC disabled for HyperOS SLA");
            }
        } catch (Throwable error) {
            log("cannot configure Cronet: " + error);
        }
    }

    private static void log(String message) {
        XposedBridge.log("[DualPathVPN] Google Photos: " + message);
    }
}
