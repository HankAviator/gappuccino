package io.github.hankaviator.gappuccino.runtime;

import io.github.libxposed.api.XposedInterface;
import java.io.IOException;
import java.lang.reflect.Executable;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CallbackTest {
    private static final class Chain implements XposedInterface.Chain {
        Object[] args = {"original"};
        Object[] received;
        int calls;
        Throwable failure;
        public Executable getExecutable() { try { return String.class.getMethod("concat", String.class); } catch (Exception e) { throw new AssertionError(e); } }
        public Object getThisObject() { return "receiver"; }
        public List<Object> getArgs() { return Collections.unmodifiableList(Arrays.asList(args)); }
        public Object getArg(int i) { return args[i]; }
        public Object proceed() throws Throwable { return proceed(args); }
        public Object proceed(Object[] changed) throws Throwable {
            assertEquals("Original must run at most once", 0, calls++);
            received = changed;
            if (failure != null) throw failure;
            return "result:" + changed[0];
        }
        public Object proceedWith(Object receiver) { throw new AssertionError("Receiver must stay unchanged"); }
        public Object proceedWith(Object receiver, Object[] changed) { throw new AssertionError("Receiver must stay unchanged"); }
    }
    @Test public void modifiedArgumentsProceedOnceAndAfterCanReplaceResult() throws Throwable {
        Chain chain = new Chain();
        Object result = new Callback() {
            protected void beforeHookedMethod(MethodHookParam p) { assertEquals("receiver", p.thisObject); p.args[0] = "changed"; }
            protected void afterHookedMethod(MethodHookParam p) { assertEquals("result:changed", p.getResult()); p.setResult("replaced"); }
        }.intercept(chain);
        assertEquals("replaced", result); assertEquals(1, chain.calls);
        assertEquals("original", chain.args[0]);
    }
    @Test public void nullEarlyReturnSkipsOriginalAndStillRunsAfter() throws Throwable {
        Chain chain = new Chain(); boolean[] after = {false};
        assertNull(new Callback() {
            protected void beforeHookedMethod(MethodHookParam p) { p.setResult(null); }
            protected void afterHookedMethod(MethodHookParam p) { after[0] = true; }
        }.intercept(chain));
        assertEquals(0, chain.calls); assertTrue(after[0]);
    }
    @Test public void originalFailureRetainsIdentity() throws Throwable {
        Chain chain = new Chain(); chain.failure = new IOException("original");
        try { new Callback() {}.intercept(chain); fail(); }
        catch (IOException error) { assertSame(chain.failure, error); }
        assertEquals(1, chain.calls);
    }
    @Test public void afterCanRecoverOriginalFailure() throws Throwable {
        Chain chain = new Chain(); chain.failure = new IOException("original");
        assertEquals("recovered", new Callback() {
            protected void afterHookedMethod(MethodHookParam p) { assertTrue(p.hasThrowable()); p.setResult("recovered"); }
        }.intercept(chain));
        assertEquals(1, chain.calls);
    }
    @Test public void failedBeforeRestoresArgumentsAndEarlyReturn() throws Throwable {
        Chain chain = new Chain();
        assertEquals("result:original", new Callback() {
            protected void beforeHookedMethod(MethodHookParam p) { p.args[0] = "bad"; p.setResult("bad"); throw new IllegalStateException("broken hook"); }
        }.intercept(chain));
        assertEquals(1, chain.calls);
    }
    @Test public void failedAfterRestoresOriginalOutcomeWithoutRepeatingOriginal() throws Throwable {
        Chain chain = new Chain();
        assertEquals("result:original", new Callback() {
            protected void afterHookedMethod(MethodHookParam p) { p.setResult("bad"); throw new IllegalStateException("broken hook"); }
        }.intercept(chain));
        assertEquals(1, chain.calls);
    }
    @Test public void intentionalDownloadBlockPropagatesWithoutOriginal() throws Throwable {
        Chain chain = new Chain(); IOException blocked = new IOException("self update blocked");
        Callback hook = new Callback(HookRuntime.PRIORITY_DEFAULT, true) {
            protected void beforeHookedMethod(MethodHookParam p) { p.setThrowable(blocked); }
        };
        assertTrue(hook.propagateFailures);
        try { hook.intercept(chain); fail(); } catch (IOException error) { assertSame(blocked, error); }
        assertEquals(0, chain.calls);
    }
}
