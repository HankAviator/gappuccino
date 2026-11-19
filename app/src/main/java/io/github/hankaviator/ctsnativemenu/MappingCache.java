package io.github.hankaviator.ctsnativemenu;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Properties;

/** Stores discovery results in the Google app's private code cache. */
final class MappingCache {
    private static final String NAME = "gappuccino-lens-mapping-v1.properties";
    private static final String MENU_VIEW =
            "com.google.android.libraries.lens.view.actionmenu.ActionMenuView";

    private MappingCache() {}

    static Hook.Mapping read(Context context, ClassLoader loader) {
        try {
            Properties data = new Properties();
            try (var input = Files.newInputStream(new File(context.getCodeCacheDir(), NAME).toPath())) {
                data.load(input);
            }
            if (!identity(context).equals(data.getProperty("apk"))) return null;
            Class<?> peer = loader.loadClass(data.getProperty("peer"));
            Class<?> controller = loader.loadClass(data.getProperty("controller"));
            Method view = method(peer, data, "view");
            Method text = method(peer, data, "text");
            Method select = method(controller, data, "select");
            Field peerOverlay = field(peer, data, "peerOverlay");
            Field controllerOverlay = field(controller, data, "controllerOverlay");
            if (!MENU_VIEW.equals(view.getReturnType().getName())
                    || text.getReturnType() != String.class || select.getReturnType() != void.class
                    || peerOverlay.getType() != controllerOverlay.getType()) return null;
            return new Hook.Mapping(controller, view, text, select, peerOverlay, controllerOverlay);
        } catch (IOException | ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    static void write(Context context, Hook.Mapping mapping) {
        File temporary = null;
        try {
            File directory = context.getCodeCacheDir();
            temporary = File.createTempFile("gappuccino-lens-", ".tmp", directory);
            Properties data = new Properties();
            data.setProperty("apk", identity(context));
            data.setProperty("peer", mapping.view().getDeclaringClass().getName());
            data.setProperty("controller", mapping.controller().getName());
            data.setProperty("view", mapping.view().getName());
            data.setProperty("text", mapping.text().getName());
            data.setProperty("select", mapping.selectAll().getName());
            data.setProperty("peerOverlay", mapping.peerOverlay().getName());
            data.setProperty("controllerOverlay", mapping.controllerOverlay().getName());
            try (var output = Files.newOutputStream(temporary.toPath())) {
                data.store(output, null);
            }
            Files.move(temporary.toPath(), new File(directory, NAME).toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException ignored) {
            // Discovery remains available if disk caching fails.
        } finally {
            if (temporary != null) temporary.delete();
        }
    }

    private static String identity(Context context) {
        ApplicationInfo info = context.getApplicationInfo();
        return info.sourceDir + "\n" + Arrays.toString(info.splitSourceDirs);
    }

    private static Method method(Class<?> owner, Properties data, String key)
            throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(data.getProperty(key));
        if (Modifier.isStatic(method.getModifiers())) throw new NoSuchMethodException(key);
        method.setAccessible(true);
        return method;
    }

    private static Field field(Class<?> owner, Properties data, String key)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(data.getProperty(key));
        if (Modifier.isStatic(field.getModifiers())) throw new NoSuchFieldException(key);
        field.setAccessible(true);
        return field;
    }
}
