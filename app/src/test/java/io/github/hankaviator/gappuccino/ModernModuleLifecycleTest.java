package io.github.hankaviator.gappuccino;

import io.github.libxposed.api.XposedModuleInterface;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModernModuleLifecycleTest {
    @Test public void manualHotReloadIsRejectedWithoutReadingOrSavingState() {
        var param=(XposedModuleInterface.HotReloadingParam)Proxy.newProxyInstance(
                getClass().getClassLoader(),new Class[]{XposedModuleInterface.HotReloadingParam.class},
                (proxy,method,args)->{throw new AssertionError("Reload state accessed: "+method);});
        assertFalse(new ModernModule().onHotReloading(param));
    }
    @Test public void noGenerationRestorationCallbackRemains() {
        assertFalse(Arrays.stream(ModernModule.class.getDeclaredMethods())
                .anyMatch(method->method.getName().equals("onHotReloaded")));
    }
}
