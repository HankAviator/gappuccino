package io.github.hankaviator.gappuccino.runtime;

import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Member;

/** Small per-invocation state for recipes needing work on both sides of proceed. */
public abstract class Callback {
    final int priority;
    final boolean propagateFailures;
    public Callback() { this(HookRuntime.PRIORITY_DEFAULT); }
    public Callback(int priority) { this(priority, false); }
    public Callback(int priority, boolean propagateFailures) { this.priority = priority; this.propagateFailures = propagateFailures; }
    protected void beforeHookedMethod(MethodHookParam call) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam call) throws Throwable {}
    final Object intercept(XposedInterface.Chain chain) throws Throwable {
        MethodHookParam call = new MethodHookParam();
        call.method = chain.getExecutable(); call.thisObject = chain.getThisObject(); call.args = chain.getArgs().toArray();
        Object[] originalArgs = call.args.clone();
        try { beforeHookedMethod(call); }
        catch (Throwable error) {
            HookRuntime.log(error); call.args = originalArgs; call.result = null; call.error = null; call.skip = false;
        }
        if (!call.skip) {
            try { call.result = chain.proceed(call.args); }
            catch (Throwable error) { call.error = error; }
        }
        Object result = call.result; Throwable failure = call.error;
        try { afterHookedMethod(call); }
        catch (Throwable error) { HookRuntime.log(error); call.result = result; call.error = failure; }
        if (call.error != null) throw call.error;
        return call.result;
    }
    public static final class MethodHookParam {
        public Object thisObject;
        public Object[] args;
        public Member method;
        private Object result;
        private Throwable error;
        private boolean skip;
        public Object getResult() { return result; }
        public Throwable getThrowable() { return error; }
        public boolean hasThrowable() { return error != null; }
        public void setResult(Object value) { result = value; error = null; skip = true; }
        public void setThrowable(Throwable value) { error = value; result = null; skip = true; }
    }
    public static final class Unhook {
        private final XposedInterface.HookHandle handle;
        Unhook(XposedInterface.HookHandle handle) { this.handle = handle; }
        public void unhook() { handle.unhook(); }
    }
}
