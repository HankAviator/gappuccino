package io.github.hankaviator.playstoreselfupdatelock;

import io.github.hankaviator.gappuccino.runtime.*;


import android.content.pm.PackageInstaller;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Prevents Google Play Store from replacing its own package while leaving
 * installation and updates of every other package untouched.
 */
public final class MainHook implements FeatureInstaller {
    private static final String PLAY_STORE = "com.android.vending";
    private static final String TAG = "PlayStoreSelfUpdateLock";
    private static final String VERSION = "1.1";

    @Override
    public void install(TargetPackage loadPackageParam) {
        if (!PLAY_STORE.equals(loadPackageParam.packageName)) {
            return;
        }

        int hookCount = 0;
        for (Method method : PackageInstaller.class.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (!"createSession".equals(method.getName())
                    || parameters.length == 0
                    || !PackageInstaller.SessionParams.class.isAssignableFrom(parameters[0])) {
                continue;
            }

            HookRuntime.hookMethod(method, new Callback(HookRuntime.PRIORITY_DEFAULT, true) {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    PackageInstaller.SessionParams sessionParams =
                            (PackageInstaller.SessionParams) param.args[0];
                    String targetPackage = getTargetPackage(sessionParams);
                    if (!PLAY_STORE.equals(targetPackage)) {
                        return;
                    }

                    HookRuntime.log(TAG + " " + VERSION
                            + ": blocked Play Store self-update session with IOException in "
                            + loadPackageParam.processName);
                    // createSession() declares IOException for a rejected session. Keep the
                    // failure inside the API contract so Play Store can handle it as an
                    // ordinary failed update instead of terminating on an unexpected,
                    // unchecked SecurityException.
                    param.setThrowable(new IOException(
                            "Play Store self-update blocked by user policy"));
                }
            });
            hookCount++;
        }

        HookRuntime.log(TAG + " " + VERSION + ": installed " + hookCount
                + " createSession hook(s) in " + loadPackageParam.processName);
    }

    private static String getTargetPackage(PackageInstaller.SessionParams sessionParams) {
        try {
            Object value = Reflection.getObjectField(sessionParams, "appPackageName");
            return value instanceof String ? (String) value : null;
        } catch (Throwable error) {
            HookRuntime.log(TAG + ": unable to inspect session target: " + error);
            return null;
        }
    }
}
