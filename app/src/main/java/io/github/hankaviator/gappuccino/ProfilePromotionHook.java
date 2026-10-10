package io.github.hankaviator.gappuccino;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Version-independent, window-local filtering of Google's account-menu offers. */
final class ProfilePromotionHook {
    private static final Map<View, Saved> HIDDEN = new WeakHashMap<>();
    private static final Map<TextView, Integer> MARKERS = new WeakHashMap<>();
    private static final Map<View, Observer> OBSERVED = new WeakHashMap<>();
    private static boolean hideAi, hidePoints;

    static void install(boolean ai, boolean points) {
        hideAi = ai; hidePoints = points;
        // Track only exact headings. No activity-wide tree walks or delayed scans.
        XposedBridge.hookAllMethods(TextView.class, "onAttachedToWindow", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) { inspect((TextView) param.thisObject); }
        });
        XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class,
                TextView.BufferType.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) { inspect((TextView) param.thisObject); }
        });
        XposedBridge.hookAllMethods(TextView.class, "onDetachedFromWindow", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                TextView view = (TextView) param.thisObject;
                if (MARKERS.containsKey(view)) markDirty(view.getRootView());
            }
        });
        XposedBridge.log("Gappuccino: pre-draw profile promotion filtering installed");
    }

    private static void inspect(TextView view) {
        // Google binds native UI on the main thread. Ignore unexpected background setters.
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper() || !view.isAttachedToWindow()) return;
        try {
            int kind = ProfilePromotionMatcher.kind(view.getText());
            Integer previous = MARKERS.get(view);
            if (kind == 0) {
                if (previous != null) { MARKERS.remove(view); observe(view.getRootView()); }
                return;
            }
            MARKERS.put(view, kind);
            // Bento cards are already identifiable during attachment/binding. Remove
            // them before the first measure can seed the menu's size animation.
            // The pre-draw observer still handles late binding and unknown containers.
            if ((hideAi && kind == ProfilePromotionMatcher.AI
                    || hidePoints && kind == ProfilePromotionMatcher.POINTS)
                    && hasBentoAncestor(view)) {
                View target = removalTarget(view);
                if (target != null) hide(target);
            }
            observe(view.getRootView());
        } catch (Throwable error) { logScanError(error); }
    }
    private static void observe(View root) {
            Observer observer = OBSERVED.get(root);
            if (observer == null) {
                observer = new Observer(root);
                OBSERVED.put(root, observer);
                root.getViewTreeObserver().addOnGlobalLayoutListener(observer);
                root.getViewTreeObserver().addOnPreDrawListener(observer);
                root.addOnAttachStateChangeListener(observer);
            }
            observer.dirty = true;
    }
    private static void markDirty(View root) {
        Observer observer = OBSERVED.get(root);
        if (observer != null) observer.dirty = true;
    }
    private static void logScanError(Throwable error) {
        XposedBridge.log("Gappuccino: profile promotion scan unavailable: " + error.getClass().getSimpleName());
    }

    private static final class Observer implements ViewTreeObserver.OnGlobalLayoutListener,
            ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
        private final WeakReference<View> root;
        boolean dirty = true;
        int passes;
        long scanNanos;
        Observer(View view) { root = new WeakReference<>(view); }
        @Override public void onGlobalLayout() { dirty = true; }
        @Override public void onViewAttachedToWindow(View view) { dirty = true; }
        @Override public void onViewDetachedFromWindow(View view) {
            // Cached card views must start with their original dimensions when reused.
            for (View hidden : new ArrayList<>(HIDDEN.keySet()))
                if (hidden != null && hidden.getRootView() == view) restore(hidden);
            close(view);
        }
        @Override public boolean onPreDraw() {
            View view = root.get();
            if (view == null || !dirty) return true;
            dirty = false;
            long start = System.nanoTime();
            try {
                ScanResult result = scan(view);
                passes++;
                scanNanos += System.nanoTime() - start;
                if (!result.hasMarkers()) close(view);
                // Visibility/layout edits must settle before any frame is presented.
                return !result.changed();
            } catch (Throwable error) {
                close(view); logScanError(error); return true;
            }
        }
        private void close(View view) {
            ViewTreeObserver tree = view.getViewTreeObserver();
            if (tree.isAlive()) {
                tree.removeOnGlobalLayoutListener(this);
                tree.removeOnPreDrawListener(this);
            }
            view.removeOnAttachStateChangeListener(this);
            OBSERVED.remove(view);
            XposedBridge.log("Gappuccino: profile pre-draw passes=" + passes + ", scan_us=" + scanNanos / 1000);
        }
    }
    private record ScanResult(boolean changed, boolean hasMarkers) {}
    private static ScanResult scan(View root) {
        ArrayList<TextView> markers = new ArrayList<>();
        boolean account = false;
        for (Map.Entry<TextView, Integer> entry : MARKERS.entrySet()) {
            TextView node = entry.getKey();
            if (node == null || !node.isAttachedToWindow() || node.getRootView() != root) continue;
            markers.add(node);
            if (entry.getValue() == ProfilePromotionMatcher.ACCOUNT) {
                for (View current = node; current != null; current = parent(current)) {
                    if (current.isClickable()) { account = true; break; }
                    if (current == root) break;
                }
            }
        }
        Set<View> targets = Collections.newSetFromMap(new WeakHashMap<>());
        for (TextView node : markers) {
            int kind = MARKERS.get(node);
            if (!(hideAi && kind == ProfilePromotionMatcher.AI) && !(hidePoints && kind == ProfilePromotionMatcher.POINTS)) continue;
            if (!account && !hasBentoAncestor(node)) continue;
            View target = removalTarget(node);
            if (target != null) targets.add(target);
        }
        boolean changed = false;
        for (View view : new ArrayList<>(HIDDEN.keySet())) {
            if (view != null && view.getRootView() == root && !targets.contains(view)) changed |= restore(view);
        }
        for (View target : targets) changed |= hide(target);
        return new ScanResult(changed, !markers.isEmpty());
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
    private static boolean hide(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) return false;
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
            return true;
        }
        return false;
    }
    private static boolean restore(View view) {
        Saved saved = HIDDEN.remove(view); if (saved == null) return false;
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
        return true;
    }
    private ProfilePromotionHook() {}
}
