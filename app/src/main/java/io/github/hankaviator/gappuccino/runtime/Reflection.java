package io.github.hankaviator.gappuccino.runtime;

import java.lang.reflect.*;
import java.util.*;

/** Reflection over target app classes, with no access to legacy framework APIs. */
public final class Reflection {
    public static Class<?> findClass(String name, ClassLoader loader) {
        try { return Class.forName(name, false, loader); }
        catch (ClassNotFoundException error) { throw new IllegalArgumentException(name, error); }
    }
    public static Class<?> findClassIfExists(String name, ClassLoader loader) {
        try { return Class.forName(name, false, loader); } catch (ClassNotFoundException ignored) { return null; }
    }
    private static Field field(Class<?> owner, String name) {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new IllegalArgumentException("No field " + owner.getName() + "." + name);
    }
    public static Object getObjectField(Object target, String name) {
        try { return field(target.getClass(), name).get(target); } catch (IllegalAccessException error) { throw new IllegalStateException(error); }
    }
    public static Object getStaticObjectField(Class<?> owner, String name) {
        try { return field(owner, name).get(null); } catch (IllegalAccessException error) { throw new IllegalStateException(error); }
    }
    public static Object callMethod(Object target, String name, Object... args) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            List<Method> matches = new ArrayList<>();
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length || method.isBridge()) continue;
                boolean match = true;
                for (int i = 0; i < args.length; i++) {
                    Class<?> parameter = boxed(method.getParameterTypes()[i]);
                    if (args[i] == null ? method.getParameterTypes()[i].isPrimitive() : !parameter.isInstance(args[i])) { match = false; break; }
                }
                if (match) matches.add(method);
            }
            if (matches.size() > 1) throw new IllegalArgumentException("Ambiguous method " + name);
            if (matches.size() == 1) {
                try { Method method = matches.get(0); method.setAccessible(true); return method.invoke(target, args); }
                catch (ReflectiveOperationException error) { throw new IllegalStateException(error instanceof InvocationTargetException ? error.getCause() : error); }
            }
        }
        throw new IllegalArgumentException("No method " + name);
    }
    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class; if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class; if (type == float.class) return Float.class;
        if (type == double.class) return Double.class; if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class; if (type == char.class) return Character.class;
        return Void.class;
    }
    public static Callback.Unhook findAndHookMethod(Class<?> owner, String name, Object... signature) {
        if (!(signature[signature.length-1] instanceof Callback callback)) throw new IllegalArgumentException("Missing interceptor");
        Class<?>[] params = new Class<?>[signature.length-1];
        for (int i = 0; i < params.length; i++) params[i] = signature[i] instanceof Class<?> type ? type : findClass((String) signature[i], owner.getClassLoader());
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            try { return HookRuntime.hookMethod(type.getDeclaredMethod(name, params), callback); }
            catch (NoSuchMethodException ignored) { }
        }
        throw new IllegalArgumentException("No method " + owner.getName() + "." + name);
    }
    public static Callback.Unhook findAndHookMethod(String owner, ClassLoader loader, String name, Object... signature) {
        return findAndHookMethod(findClass(owner, loader), name, signature);
    }
    private Reflection() {}
}
