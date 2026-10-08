package io.github.hankaviator.asimultilingual;

import android.content.Context;
import android.os.CancellationSignal;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextSelection;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** ASI keeps the first response; Android's multilingual classifier fills misses. */
public final class Hook implements IXposedHookLoadPackage {
    private static final ExecutorService WORK = Executors.newFixedThreadPool(2);
    private static volatile TextClassifier fallback;

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) throws Throwable {
        if (!"com.google.android.as".equals(load.packageName)) return;
        Class<?> service = Class.forName("com.google.android.apps.miphone.aiai.textclassifier.service.QAiaiTextClassifierService", false, load.classLoader);
        for (String operation : new String[]{"onClassifyText", "onSuggestSelection"}) {
            XposedBridge.hookAllMethods(service, operation, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam call) throws Throwable {
                    if (call.args.length != 4 || !(call.thisObject instanceof Context)) return;
                    Context context = (Context) call.thisObject;
                    Object request = call.args[1];
                    CancellationSignal cancellation = (CancellationSignal) call.args[2];
                    Object original = call.args[3];
                    Class<?> callbackType = ((Method) call.method).getParameterTypes()[3];
                    call.args[3] = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[]{callbackType}, (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            if (method.getName().equals("toString")) return "AsiMultilingualCallback";
                            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                            return proxy == args[0];
                        }
                        if (!method.getName().equals("onSuccess") && !method.getName().equals("onFailure")) return null;
                        Object primary = method.getName().equals("onSuccess") ? args[0] : null;
                        if (useful(primary)) { method.invoke(original, args); return null; }
                        WORK.execute(() -> {
                            if (cancellation != null && cancellation.isCanceled()) return;
                            Object result = primary;
                            try {
                                TextClassifier classifier = defaultClassifier(context);
                                Object candidate = request instanceof TextClassification.Request
                                    ? classifier.classifyText((TextClassification.Request) request)
                                    : classifier.suggestSelection((TextSelection.Request) request);
                                if (useful(candidate)) result = candidate;
                            } catch (Throwable error) {
                                XposedBridge.log("ASI Multilingual: fallback failed: " + error.getClass().getSimpleName());
                            }
                            if (cancellation != null && cancellation.isCanceled()) return;
                            try {
                                if (result != null) callbackType.getMethod("onSuccess", Object.class).invoke(original, result);
                                else method.invoke(original, args);
                            } catch (Throwable error) {
                                XposedBridge.log("ASI Multilingual: callback failed: " + error.getClass().getSimpleName());
                            }
                        });
                        return null;
                    });
                }
            });
        }
        XposedBridge.log("ASI Multilingual: classifier and selection fallback installed");
    }

    private static synchronized TextClassifier defaultClassifier(Context context) throws Exception {
        if (fallback == null) {
            Class<?> service = Class.forName("android.service.textclassifier.TextClassifierService");
            Method method = service.getDeclaredMethod("getDefaultTextClassifierImplementation", Context.class);
            method.setAccessible(true);
            fallback = (TextClassifier) method.invoke(null, context);
        }
        return fallback;
    }

    private static boolean useful(Object result) {
        if (result instanceof TextClassification) {
            TextClassification classification = (TextClassification) result;
            if (classification.getActions().isEmpty()) return false;
            for (int i = 0; i < classification.getEntityCount(); i++) {
                String entity = classification.getEntity(i);
                if (supported(entity) && classification.getConfidenceScore(entity) >= .5f) return true;
            }
        } else if (result instanceof TextSelection) {
            TextSelection selection = (TextSelection) result;
            for (int i = 0; i < selection.getEntityCount(); i++) {
                String entity = selection.getEntity(i);
                if (supported(entity) && selection.getConfidenceScore(entity) >= .5f) return true;
            }
        }
        return false;
    }

    private static boolean supported(String entity) {
        // Preserve ASI's richer entities too; only empty/translation results need help.
        return !entity.equals("other") && !entity.equals("translate") && !entity.isEmpty();
    }
}
