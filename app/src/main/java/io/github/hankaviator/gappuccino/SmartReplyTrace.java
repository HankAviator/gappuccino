package io.github.hankaviator.gappuccino;

import android.util.Log;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Opt-in metadata tracing. Never records conversation text or proposed replies. */
public final class SmartReplyTrace {
    private static final String TAG = "GappuccinoSmartReply";
    public static void install(ClassLoader loader) {
        observe(loader, "com.google.android.apps.miphone.aiai.app.AiAiContentCaptureService", "onCreateContentCaptureSession");
        observe(loader, "com.google.android.apps.miphone.aiai.app.AiAiContentCaptureService", "onContentCaptureEvent");
        observe(loader, "com.google.android.apps.miphone.aiai.app.AiAiAugmentedAutofillService", "onFillRequest");
        observe(loader, "com.google.android.apps.miphone.aiai.textclassifier.service.QAiaiTextClassifierService", "onSuggestConversationActions");
        observe(loader, "android.service.autofill.augmented.FillCallback", "onSuccess");
        observe(loader, "android.service.autofill.augmented.FillResponse$Builder", "build");
    }
    static void observe(ClassLoader loader, String name, String method) {
        try {
            Class<?> type = XposedHelpers.findClass(name, loader);
            AtomicInteger calls = new AtomicInteger();
            var hooks = XposedBridge.hookAllMethods(type, method, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam call) {
                    int count = calls.incrementAndGet();
                    // Bound event logging; keep counts without recording captured content.
                    if (count > 10 && count % 100 != 0) return;
                    String suffix = call.hasThrowable() ? " failed=" + call.getThrowable().getClass().getSimpleName() : "";
                    if (method.equals("onSuccess") && call.args.length > 0)
                        suffix += " response=" + (call.args[0] == null ? "null" : call.args[0].getClass().getSimpleName());
                    if (name.equals("cxo") && call.args.length > 0) {
                        Object messages = XposedHelpers.getObjectField(call.args[0], "c");
                        suffix += " parsedMessages=" + (messages instanceof Collection ? ((Collection<?>) messages).size() : 0)
                                + " snapshotState=" + XposedHelpers.getObjectField(call.thisObject, "b");
                    }
                    if (name.equals("cyk") && call.getResult() instanceof Collection)
                        suffix += " candidateProviders=" + ((Collection<?>) call.getResult()).size();
                    XposedBridge.log(TAG + ": " + type.getSimpleName() + "." + method + " count=" + count + suffix);
                }
            });
            XposedBridge.log(TAG + ": " + "trace installed " + type.getSimpleName() + "." + method + " hooks=" + hooks.size());
        } catch (Throwable error) { XposedBridge.log(TAG + ": " + "trace unavailable " + method + ": " + error.getClass().getSimpleName()); }
    }
    private SmartReplyTrace() {}
}
