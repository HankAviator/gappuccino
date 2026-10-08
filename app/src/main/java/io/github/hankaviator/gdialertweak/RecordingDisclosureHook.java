package io.github.hankaviator.gdialertweak;

import android.content.Context;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Semantic DEX discovery: no Phone version checks or obfuscated class/method names. */
final class RecordingDisclosureHook {
    private RecordingDisclosureHook() {}

    static void install(Context context, ClassLoader loader, BooleanSupplier enabled) {
        List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
        try {
            System.loadLibrary("dexkit");
            try (DexKitBridge bridge = DexKitBridge.create(context.getApplicationInfo().sourceDir)) {
                Class<?> rawAudio = unique(bridge, loader, "RawInjectableAudio(audioFile=");
                Constructor<?> rawConstructor = rawAudio.getDeclaredConstructor(File.class);
                rawConstructor.setAccessible(true);
                Class<?> pair = unique(bridge, loader, "CallRecordingDisclosure(startingAudio=");
                List<Constructor<?>> pairConstructors = new ArrayList<>();
                for (Constructor<?> constructor : pair.getDeclaredConstructors()) {
                    Class<?>[] args = constructor.getParameterTypes();
                    if (args.length == 2 && args[0] == args[1] && args[0].isInterface()
                            && args[0].isAssignableFrom(rawAudio)) pairConstructors.add(constructor);
                }
                if (pairConstructors.size() != 1) throw new IllegalStateException("Ambiguous disclosure payload");

                // Older playback pipelines return a path through a recording-only provider.
                Class<?> tts = unique(bridge, loader,
                        "com/android/dialer/callrecording/disclosure/impl/TtsCallRecordingDisclosure");
                List<Class<?>> providerInterfaces = new ArrayList<>();
                for (Class<?> type : tts.getInterfaces()) {
                    Method[] methods = type.getDeclaredMethods();
                    if (methods.length == 2 && isFutureProvider(methods[0])
                            && isFutureProvider(methods[1])) providerInterfaces.add(type);
                }
                if (providerInterfaces.size() != 1) throw new IllegalStateException("Ambiguous disclosure provider");
                Class<?> providerInterface = providerInterfaces.get(0);
                List<Method> providerMethods = new ArrayList<>();
                for (ClassData data : bridge.findClass(FindClass.create().matcher(
                        ClassMatcher.create().addInterface(providerInterface.getName())))) {
                    Class<?> provider = data.getInstance(loader);
                    for (Method contract : providerInterface.getDeclaredMethods()) {
                        Method method = provider.getMethod(contract.getName());
                        if (!Modifier.isAbstract(method.getModifiers()) && isFutureProvider(method)) {
                            method.setAccessible(true);
                            providerMethods.add(method);
                        }
                    }
                }
                if (providerMethods.isEmpty()) throw new IllegalStateException("No disclosure providers");

                File silence = DisclosureAudio.createSilence(context.getFilesDir());
                Object silentAudio = rawConstructor.newInstance(silence);
                hooks.add(XposedBridge.hookMethod(pairConstructors.get(0), new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (!enabled.getAsBoolean()) return;
                        param.args[0] = silentAudio;
                        param.args[1] = silentAudio;
                    }
                }));
                for (Method method : providerMethods) {
                    hooks.add(XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            if (param.hasThrowable() || param.getResult() == null
                                    || !enabled.getAsBoolean()) return;
                            param.setResult(DisclosureAudio.wrapFuture(method.getReturnType(),
                                    param.getResult(), silence, enabled));
                        }
                    }));
                }
                log("generic disclosure audio hooks installed: payload=" + pair.getName()
                        + ", providers=" + providerMethods.size());
            }
        } catch (Throwable error) {
            // Remove partial installation and retain Google's normal disclosure behavior.
            for (XC_MethodHook.Unhook hook : hooks) hook.unhook();
            log("disclosure suppression unavailable; original audio retained: " + error);
        }
    }

    private static boolean isFutureProvider(Method method) {
        return method.getParameterCount() == 0 && method.getReturnType().isInterface()
                && Future.class.isAssignableFrom(method.getReturnType());
    }

    private static Class<?> unique(DexKitBridge bridge, ClassLoader loader, String marker)
            throws ClassNotFoundException {
        List<ClassData> matches = bridge.findClass(FindClass.create().matcher(
                ClassMatcher.create().usingStrings(List.of(marker), StringMatchType.Contains, false)));
        if (matches.size() != 1) throw new IllegalStateException("Expected one class for " + marker
                + ", found " + matches.size());
        return matches.get(0).getInstance(loader);
    }

    private static void log(String message) { XposedBridge.log("GDialerTweak: " + message); }
}
