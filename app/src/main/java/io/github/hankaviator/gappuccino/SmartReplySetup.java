package io.github.hankaviator.gappuccino;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Installs the resource-only Magisk companion shipped inside this APK. */
public final class SmartReplySetup {
    public static String configure(Context context, boolean enabled) throws Exception {
        File directory = new File(context.getCacheDir(), "providers");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new java.io.IOException("Cannot stage provider files");
        for (String name : new String[]{"install.sh", "module.prop", "service.sh", "GappuccinoProviders.apk"}) {
            try (InputStream input = context.getAssets().open("providers/" + name);
                 FileOutputStream output = new FileOutputStream(new File(directory, name))) {
                copy(input, output);
            }
        }
        // The fixed app-private path has no user-controlled shell content.
        Process process = new ProcessBuilder("su", "-c", "sh '" + new File(directory, "install.sh").getAbsolutePath()
                + "' " + (enabled ? "enable" : "disable")).redirectErrorStream(true).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new java.io.IOException("Root setup timed out"); }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        copy(process.getInputStream(), result);
        String message = new String(result.toByteArray(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) throw new java.io.IOException(message.trim());
        return "Saved. Reboot the phone to apply Smart Reply providers.";
    }
    private static void copy(InputStream input, OutputStream output) throws java.io.IOException {
        byte[] buffer = new byte[8192];
        for (int length; (length = input.read(buffer)) != -1;) output.write(buffer, 0, length);
    }
    private SmartReplySetup() {}
}
