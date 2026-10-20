package io.github.hankaviator.gappuccino;

import io.github.hankaviator.gappuccino.runtime.*;


import android.app.*;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
final class KeepDeleteHook {
    private static final Set<Dialog> CONFIRM = Collections.newSetFromMap(new WeakHashMap<>());
    static void install() {
        Reflection.findAndHookMethod(Application.class, "attach", Context.class, new Callback() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                try {
                    Context context = (Context) param.args[0];
                    var mapping = KeepDeleteDiscovery.resolve(new File(context.getApplicationInfo().sourceDir));
                    Class<?> fragment = Class.forName(mapping.owner().substring(1, mapping.owner().length()-1).replace('/', '.'), false, context.getClassLoader());
                    Field arguments = null;
                    for (Class<?> type = fragment; type != null && arguments == null; type = type.getSuperclass()) {
                        try { arguments = type.getDeclaredField(mapping.arguments()); } catch (NoSuchFieldException ignored) { }
                    }
                    if (arguments == null || arguments.getType() != Bundle.class) throw new IllegalStateException("Missing fragment arguments");
                    arguments.setAccessible(true);
                    Field field = arguments;
                    HookRuntime.hookMethod(fragment.getDeclaredMethod(mapping.createDialog(), Bundle.class), new Callback() {
                        @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Bundle args = (Bundle) field.get(param.thisObject);
                            if (args == null || !(param.getResult() instanceof Dialog dialog)) return;
                            for (String key : args.keySet()) {
                                if (key.endsWith("_requestCode") && KeepDeleteDiscovery.ordinaryTrash(args.getString(key))) {
                                    CONFIRM.add(dialog); break;
                                }
                            }
                        }
                    });
                    Reflection.findAndHookMethod(Dialog.class, "show", new Callback() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            Dialog dialog = (Dialog) param.thisObject;
                            if (!CONFIRM.remove(dialog)) return;
                            View positive = dialog.findViewById(android.R.id.button1);
                            if (positive == null || !positive.isEnabled() || dialog.getWindow() == null) return;
                            View decor = dialog.getWindow().getDecorView();
                            int visibility = decor.getVisibility();
                            decor.setVisibility(View.INVISIBLE);
                            if (!positive.performClick()) decor.setVisibility(visibility);
                            else HookRuntime.log("Gappuccino: confirmed Keep move-to-trash request");
                        }
                    });
                    HookRuntime.log("Gappuccino: Keep delete confirmation hook installed");
                } catch (Throwable error) { HookRuntime.log("Gappuccino: Keep confirmation unavailable: " + error); }
            }
        });
    }
    private KeepDeleteHook() {}
}
