package io.github.hankaviator.gappuccino;

import io.github.hankaviator.gappuccino.runtime.*;


import android.app.Application;
import android.content.Context;
import android.view.*;
import android.widget.TextView;
import java.util.*;
/** Localized native UI filtering; never changes profile or health records. */
final class FitPeriodHook {
    private static final Set<String> LABELS = new HashSet<>();
    private static final Map<TextView, View> TARGETS = new WeakHashMap<>();
    private static final Map<View, Saved> HIDDEN = new WeakHashMap<>();
    private record Saved(int visibility, int height) {}
    static void install() {
        Reflection.findAndHookMethod(Application.class, "attach", Context.class, new Callback() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Context context = (Context) param.args[0];
                for (String key : List.of("period_label", "browse_cycle_tracking_category_title", "add_period_title", "menstruation_label")) {
                    int id = context.getResources().getIdentifier(key, "string", context.getPackageName());
                    if (id != 0) LABELS.add(context.getString(id));
                }
            }
        });
        Callback inspect = new Callback() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                TextView view = (TextView) param.thisObject;
                if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper() || !view.isAttachedToWindow()) return;
                View previous = TARGETS.get(view);
                boolean match = view.getText() != null && LABELS.contains(view.getText().toString());
                if (!match) { if (previous != null) { TARGETS.remove(view); restore(previous); } return; }
                View target = target(view);
                if (target == null || target.getLayoutParams() == null) return;
                TARGETS.put(view, target);
                var params = target.getLayoutParams();
                HIDDEN.putIfAbsent(target, new Saved(target.getVisibility(), params.height));
                if (target.getVisibility() != View.GONE || params.height != 0) {
                    params.height = 0; target.setLayoutParams(params); target.setVisibility(View.GONE);
                    HookRuntime.log("Gappuccino: hid Fit period entry");
                }
            }
        };
        HookRuntime.hookAllMethods(TextView.class, "onAttachedToWindow", inspect);
        Reflection.findAndHookMethod(TextView.class, "setText", CharSequence.class, TextView.BufferType.class, inspect);
        HookRuntime.hookAllMethods(TextView.class, "onDetachedFromWindow", new Callback() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                View target = TARGETS.remove((TextView) param.thisObject); if (target != null) restore(target);
            }
        });
    }
    private static View target(TextView view) {
        View node = view;
        for (int depth = 0; node != null && depth < 8; depth++) {
            if (node.isClickable()) return node;
            ViewParent parent = node.getParent();
            if (!(parent instanceof View next) || next.getParent() == null) return null;
            node = next;
        }
        return null;
    }
    private static void restore(View view) {
        Saved saved = HIDDEN.remove(view); if (saved == null) return;
        var params = view.getLayoutParams();
        if (params != null) { params.height = saved.height(); view.setLayoutParams(params); }
        view.setVisibility(saved.visibility());
    }
    private FitPeriodHook() {}
}
