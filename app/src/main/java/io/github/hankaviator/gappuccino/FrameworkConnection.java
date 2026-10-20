package io.github.hankaviator.gappuccino;

import android.content.*;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.util.Xml;
import io.github.libxposed.service.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Module-app service bridge. No framework APIs or writable preferences in target apps. */
public final class FrameworkConnection {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Set<Runnable> LISTENERS = new HashSet<>();
    private static volatile XposedService service;
    private static volatile boolean ready;
    private static volatile String status = "LSPosed service not connected";
    private static SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
    public static void start(Context context) {
        Context app = context.getApplicationContext();
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override public void onServiceBind(XposedService connected) {
                service = connected; status = "Preparing framework settings…"; notifyUi();
                if (app.getSystemService(UserManager.class).isUserUnlocked()) synchronize(app);
            }
            @Override public void onServiceDied(XposedService disconnected) {
                if (service == disconnected) { service = null; ready = false; status = "LSPosed service disconnected"; notifyUi(); }
            }
        });
    }
    public static void synchronize(Context owner) {
        Context context = owner.getApplicationContext();
        IO.execute(() -> {
            XposedService current = service;
            if (current == null) return;
            try {
                if (current.getApiVersion() < 102) throw new IllegalStateException("LSPosed API 102 or later required");
                SharedPreferences local = FeatureSettings.open(context);
                if (!local.getBoolean("_modern_migrated", false)) {
                    Map<String, Boolean> legacy = readLegacy(context);
                    var edit = local.edit();
                    legacy.forEach(edit::putBoolean);
                    if (!edit.putBoolean("_modern_migrated", true).commit())
                        throw new IOException("Could not save migrated settings");
                }
                FeatureSettings.ensureDefaults(context);
                if (preferenceListener == null) {
                    preferenceListener = (prefs, key) -> publish(context);
                    local.registerOnSharedPreferenceChangeListener(preferenceListener);
                }
                publishNow(context, current);
                ready = true;
                status = current.getFrameworkName() + " " + current.getFrameworkVersion() + " · API " + current.getApiVersion();
                notifyUi();
            } catch (Exception error) {
                ready = false; status = "Settings connection failed: " + error.getMessage(); notifyUi();
                android.util.Log.e("Gappuccino", "Framework settings unavailable", error);
            }
        });
    }
    private static void publish(Context context) {
        IO.execute(() -> {
            XposedService current = service;
            if (current == null) return;
            try { publishNow(context, current); }
            catch (RuntimeException error) { ready = false; status = "Settings sync failed: " + error.getMessage(); notifyUi(); }
        });
    }
    private static void publishNow(Context context, XposedService current) {
        var edit = current.getRemotePreferences(FeatureSettings.FILE).edit().clear();
        FeatureSettings.open(context).getAll().forEach((key, value) -> {
            if (value instanceof Boolean flag) edit.putBoolean(key, flag);
        });
        edit.putBoolean("_modern_ready", true);
        if (!edit.commit()) throw new IllegalStateException("Framework did not save settings");
    }
    private static Map<String, Boolean> readLegacy(Context context) throws Exception {
        var info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        if (info.firstInstallTime == info.lastUpdateTime) return Map.of();
        // Legacy LSPosed redirects MODE_WORLD_READABLE to a randomized directory.
        // Import only this module's own file, once; never read another module's prefs.
        String listing;
        try {
            listing = root("find /data/misc -maxdepth 6 -type f -path '*/prefs/io.github.hankaviator.gappuccino/features.xml' 2>/dev/null");
        } catch (IOException error) {
            throw new IOException("Grant Gappuccino root access in your root manager, then retry to import existing switches", error);
        }
        String newest = null; long newestTime = -1;
        for (String path : listing.split("\\R")) {
            if (!path.matches("/data/misc/[A-Za-z0-9_./-]+/prefs/io\\.github\\.hankaviator\\.gappuccino/features\\.xml") || path.contains("..")) continue;
            long modified = Long.parseLong(root("stat -c %Y '" + path + "'").trim());
            if (modified > newestTime) { newest = path; newestTime = modified; }
        }
        if (newest == null) return Map.of();
        return parseLegacy(root("cat '" + newest + "'"));
    }
    static Map<String, Boolean> parseLegacy(String xml) throws Exception {
        Map<String, Boolean> result = new HashMap<>();
        var parser = Xml.newPullParser(); parser.setInput(new StringReader(xml));
        for (int event = parser.getEventType(); event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG && parser.getName().equals("boolean")) {
                String key = parser.getAttributeValue(null, "name"), value = parser.getAttributeValue(null, "value");
                if (key != null && ("true".equals(value) || "false".equals(value))) result.put(key, Boolean.valueOf(value));
            }
        }
        if (result.isEmpty()) throw new IllegalStateException("Legacy settings were empty or unreadable");
        return result;
    }
    private static String root(String command) throws Exception {
        Process process = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
        FutureTask<byte[]> output = new FutureTask<>(() -> {
            try (var input = process.getInputStream(); var bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                for (int count; (count = input.read(buffer)) != -1;) {
                    if (bytes.size() + count > 262144) throw new IOException("Legacy settings output too large");
                    bytes.write(buffer, 0, count);
                }
                return bytes.toByteArray();
            }
        });
        Thread reader = new Thread(output, "settings-import"); reader.setDaemon(true); reader.start();
        if (!process.waitFor(15, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IOException("Legacy settings import timed out"); }
        String result = new String(output.get(2, TimeUnit.SECONDS), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) throw new IOException("Could not read legacy settings with root");
        return result;
    }
    public static boolean isReady() { return ready; }
    public static String status() { return status; }
    public static void listen(Runnable listener) { LISTENERS.add(listener); }
    public static void unlisten(Runnable listener) { LISTENERS.remove(listener); }
    private static void notifyUi() { MAIN.post(() -> { for (Runnable listener : new ArrayList<>(LISTENERS)) listener.run(); }); }
    private FrameworkConnection() {}
}
