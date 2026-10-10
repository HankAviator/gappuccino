package io.github.hankaviator.gappuccino;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Version-independent, window-local filtering of Google's account-menu offers. */
final class ProfilePromotionHook {
    private static final Map<View, Saved> HIDDEN = new WeakHashMap<>();
    private static final Set<View> OBSERVED = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean hideAi, hidePoints;

    static void install(boolean ai, boolean points) {
        hideAi = ai; hidePoints = points;
        XposedBridge.hookAllMethods(Activity.class, "onPostResume", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                observe(((Activity) param.thisObject).getWindow().getDecorView());
            }
        });
        XposedBridge.hookAllMethods(Dialog.class, "show", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Dialog dialog = (Dialog) param.thisObject;
                if (dialog.getWindow() != null) observe(dialog.getWindow().getDecorView());
            }
        });
        XposedBridge.log("Gappuccino: profile promotion filtering installed");
    }

    private static void observe(View root) {
        if (OBSERVED.add(root)) {
            WeakReference<View> reference = new WeakReference<>(root);
            Runnable scan = () -> { View view = reference.get(); if (view != null) safeScan(view); };
            root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                View view = reference.get();
                if (view != null) { view.removeCallbacks(scan); view.postDelayed(scan, 100); }
            });
            for (long delay : new long[]{0, 300, 1000, 2500}) root.postDelayed(scan, delay);
        }
        safeScan(root);
    }

    private static void safeScan(View root) {
        try { scan(root); }
        catch (Throwable error) {
            // A Google UI change must not crash its host application.
            XposedBridge.log("Gappuccino: profile promotion scan unavailable: " + error.getClass().getSimpleName());
        }
    }

    private static void scan(View root) {
        if (!root.isAttachedToWindow()) return;
        ArrayList<View> nodes = new ArrayList<>();
        collect(root, nodes, 0);
        // Obfuscated Play Store resource IDs need a second, explicit account-menu anchor.
        boolean account = false;
        for (View node : nodes) {
            if (node instanceof TextView && ProfilePromotionMatcher.account(((TextView) node).getText())) {
                for (View current = node; current != null; current = parent(current)) {
                    if (current.isClickable()) { account = true; break; }
                    if (current == root) break;
                }
            }
        }
        Set<View> targets = Collections.newSetFromMap(new WeakHashMap<>());
        for (View node : nodes) {
            if (!(node instanceof TextView)) continue;
            CharSequence title = ((TextView) node).getText();
            if (!(hideAi && ProfilePromotionMatcher.ai(title)) && !(hidePoints && ProfilePromotionMatcher.points(title))) continue;
            if (!account && !hasBentoAncestor(node)) continue;
            View target = removalTarget(node);
            if (target != null) targets.add(target);
        }
        for (View view : new ArrayList<>(HIDDEN.keySet())) {
            // Other open windows own their hidden cards; do not restore them here.
            if (view != null && view.getRootView() == root && !targets.contains(view)) restore(view);
        }
        for (View target : targets) hide(target);
    }

    private static void collect(View view, ArrayList<View> nodes, int depth) {
        if (depth > 48 || nodes.size() >= 6000) return;
        nodes.add(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), nodes, depth + 1);
        }
    }
    private static boolean hasBentoAncestor(View view) {
        for (int i = 0; view != null && i < 16; i++, view = parent(view)) {
            try {
                if (view.getId() != View.NO_ID && view.getResources().getResourceEntryName(view.getId()).startsWith("og_bento_")) return true;
            } catch (Exception ignored) { }
        }
        return false;
    }
    private static View removalTarget(View marker) {
        View child = marker;
        for (int depth = 0; depth < 12; depth++) {
            View group = parent(child);
            if (group == null) return null;
            if (isRecycler(group)) {
                // A bento stack containing one card can be removed as a whole, including its margins.
                View outer = parent(group);
                if (outer != null && isCard(outer) && singleItem(group)) return outer;
                return child;
            }
            child = group;
        }
        return null; // Unknown/Compose menus retain their original behavior.
    }
    private static boolean singleItem(View recycler) {
        try {
            java.lang.reflect.Method getAdapter = recycler.getClass().getMethod("getAdapter");
            getAdapter.setAccessible(true);
            Object adapter = getAdapter.invoke(recycler);
            if (adapter == null) return false;
            java.lang.reflect.Method getCount = adapter.getClass().getMethod("getItemCount");
            getCount.setAccessible(true);
            return (Integer) getCount.invoke(adapter) == 1;
        } catch (Exception ignored) { return false; }
    }
    private static boolean isRecycler(View view) {
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().endsWith(".RecyclerView")) return true;
        return false;
    }
    private static boolean isCard(View view) {
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().endsWith(".CardView")) return true;
        return false;
    }
    private static View parent(View view) {
        ViewParent parent = view.getParent(); return parent instanceof View ? (View) parent : null;
    }
    private record Saved(int visibility, int height, int topMargin, int bottomMargin) {}
    private static void hide(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) return;
        if (!HIDDEN.containsKey(view)) {
            ViewGroup.MarginLayoutParams margins = params instanceof ViewGroup.MarginLayoutParams ? (ViewGroup.MarginLayoutParams) params : null;
            HIDDEN.put(view, new Saved(view.getVisibility(), params.height, margins == null ? 0 : margins.topMargin, margins == null ? 0 : margins.bottomMargin));
            XposedBridge.log("Gappuccino: hid profile promotion card");
        }
        if (view.getVisibility() != View.GONE || params.height != 0) {
            params.height = 0;
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) params).topMargin = 0;
                ((ViewGroup.MarginLayoutParams) params).bottomMargin = 0;
            }
            view.setLayoutParams(params); view.setVisibility(View.GONE);
        }
    }
    private static void restore(View view) {
        Saved saved = HIDDEN.remove(view); if (saved == null) return;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.height = saved.height();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) params).topMargin = saved.topMargin();
                ((ViewGroup.MarginLayoutParams) params).bottomMargin = saved.bottomMargin();
            }
            view.setLayoutParams(params);
        }
        view.setVisibility(saved.visibility());
    }
    private ProfilePromotionHook() {}
}
