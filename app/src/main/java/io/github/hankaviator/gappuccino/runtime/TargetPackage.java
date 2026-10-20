package io.github.hankaviator.gappuccino.runtime;

import android.content.pm.ApplicationInfo;

public final class TargetPackage {
    public final String packageName, processName;
    public final ClassLoader classLoader;
    public final ApplicationInfo appInfo;
    public TargetPackage(String pkg, String process, ClassLoader loader, ApplicationInfo info) {
        packageName = pkg; processName = process; classLoader = loader; appInfo = info;
    }
}
