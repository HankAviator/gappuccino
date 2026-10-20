package io.github.hankaviator.gappuccino.runtime;

import android.content.SharedPreferences;
import java.util.Map;

/** Framework-backed, automatically updated, read-only preferences in target apps. */
public final class HookPreferences {
    private final SharedPreferences prefs;
    public HookPreferences(String owner, String group) {
        if (!owner.equals("io.github.hankaviator.gappuccino")) throw new IllegalArgumentException("Unexpected settings owner");
        prefs = HookRuntime.api.getRemotePreferences(group);
    }
    public void reload() { /* RemotePreferences updates its cache through the framework. */ }
    public boolean getBoolean(String key, boolean fallback) { return prefs.getBoolean(key, fallback); }
    public Map<String, ?> getAll() { return prefs.getAll(); }
}
