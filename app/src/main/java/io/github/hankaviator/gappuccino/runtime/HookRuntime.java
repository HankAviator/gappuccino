package io.github.hankaviator.gappuccino.runtime;

import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.*;
import java.util.*;

/** All target-process interception is owned by the modern framework instance. */
public final class HookRuntime {
    public static XposedInterface api;
    public static final int PRIORITY_DEFAULT = XposedInterface.PRIORITY_DEFAULT;
    public static Set<Callback.Unhook> hookAllMethods(Class<?> owner, String name, Callback callback) {
        Set<Callback.Unhook> result = new HashSet<>();
        for (Method method : owner.getDeclaredMethods()) if (method.getName().equals(name)) result.add(hookMethod(method, callback));
        return result;
    }
    public static Set<Callback.Unhook> hookAllConstructors(Class<?> owner, Callback callback) {
        Set<Callback.Unhook> result = new HashSet<>();
        for (Constructor<?> constructor : owner.getDeclaredConstructors()) result.add(hookMethod(constructor, callback));
        return result;
    }
    public static Callback.Unhook hookMethod(Member member, Callback callback) {
        if (!(member instanceof Executable executable)) throw new IllegalArgumentException("Not executable: " + member);
        // API 102 ids make repeated registration atomic instead of stacking duplicates.
        var handle = api.hook(executable).setId(callback.getClass().getName())
                .setPriority(callback.priority)
                .setExceptionMode(callback.propagateFailures ? XposedInterface.ExceptionMode.PASSTHROUGH : XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(callback::intercept);
        return new Callback.Unhook(handle);
    }
    public static Object invokeOriginalMethod(Member member, Object receiver, Object[] args) throws Throwable {
        try {
            if (member instanceof Method method)
                return api.getInvoker(method).setType(XposedInterface.Invoker.Type.ORIGIN).invoke(receiver, args);
            throw new IllegalArgumentException("Not a method: " + member);
        } catch (InvocationTargetException error) { throw error.getCause(); }
    }
    public static void log(String message) {
        if (api != null) api.log(android.util.Log.INFO, "Gappuccino", message);
        else System.err.println(message);
    }
    public static void log(Throwable error) {
        if (api != null) api.log(android.util.Log.ERROR, "Gappuccino", error.toString(), error);
        else error.printStackTrace();
    }
    private HookRuntime() {}
}
