package io.github.hankaviator.gappuccino;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** V41-specific locale eligibility repair; conversation language checks stay intact. */
public final class SmartReplyHook {
    public static void install(ClassLoader loader, boolean localeFallback, boolean trace, boolean telegramCapture) {
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam call) throws Throwable {
                Context context = (Context) call.args[0];
                if (!context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName
                        .equals("V.41.playstore.oemfull.843410720")) {
                    Log.i("GappuccinoSmartReply", "Version-specific Smart Reply hooks skipped for unknown ASI version");
                    return;
                }
                if (localeFallback) {
                    Class<?> gate = XposedHelpers.findClass("cvb", loader);
                    Object english = XposedHelpers.getStaticObjectField(gate, "a");
                    XposedHelpers.findAndHookMethod(gate, "a", XposedHelpers.findClass("cvf", loader),
                            java.util.Set.class, XposedHelpers.findClass("fht", loader), new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!Boolean.FALSE.equals(param.getResult())) return;
                            Object result = XposedBridge.invokeOriginalMethod((Method) param.method, null,
                                    new Object[]{param.args[0], param.args[1], english});
                            if (Boolean.TRUE.equals(result)) param.setResult(true);
                        }
                    });
                    Log.i("GappuccinoSmartReply", "Unsupported UI locale can use supported English models; conversation language checks retained");
                }
                if (trace) {
                    SmartReplyTrace.observe(loader, "cxo", "c");
                    SmartReplyTrace.observe(loader, "cyk", "a");
                }
                if (telegramCapture) {
                    XposedBridge.hookAllMethods(XposedHelpers.findClass("android.service.contentcapture.ContentCaptureService", loader),
                            "setContentCaptureWhitelist", new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            java.util.Set<String> packages = new java.util.HashSet<>();
                            if (param.args[0] instanceof java.util.Set) packages.addAll((java.util.Set<String>) param.args[0]);
                            packages.add("org.telegram.messenger");
                            param.args[0] = packages;
                        }
                    });
                    XposedBridge.hookAllMethods(android.view.autofill.AutofillManager.class,
                            "setAugmentedAutofillWhitelist", new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            java.util.Set<String> packages = new java.util.HashSet<>();
                            if (param.args[0] instanceof java.util.Set) packages.addAll((java.util.Set<String>) param.args[0]);
                            packages.add("org.telegram.messenger");
                            param.args[0] = packages;
                        }
                    });
                    XposedBridge.log("GappuccinoSmartReply: experimental official Telegram allowlisting installed; parsing support not implied");
                }
            }
        });
    }
    private SmartReplyHook() {}
}
