package io.github.hankaviator.ctsnativemenu;

import android.app.SearchManager;
import android.app.RemoteAction;
import android.app.ActivityOptions;
import android.content.*;
import android.content.pm.*;
import android.graphics.Rect;
import android.view.*;
import android.view.textclassifier.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Hook implements IXposedHookLoadPackage {
    private static final String GOOGLE = "com.google.android.googlequicksearchbox";
    private static final WeakHashMap<View, State> states = new WeakHashMap<>();
    private static final List<WeakReference<Object>> controllers = new ArrayList<>();
    private static final ExecutorService classificationExecutor = Executors.newFixedThreadPool(2);
    private static Mapping mapping;
    private static boolean installed;
    private record Mapping(Class<?> controller, Method view, Method text, Method selectAll,
                           Field peerOverlay, Field controllerOverlay) {}

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) throws Throwable {
        if (!GOOGLE.equals(p.packageName)) return;
        XposedHelpers.findAndHookMethod(android.app.Application.class, "attach", Context.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam h) {
                try {
                    Context c = (Context) h.args[0];
                    if (installed) return;
                    installed = true;
                    install(c, p.classLoader);
                } catch (Throwable e) { XposedBridge.log("CircleNativeMenu: generic discovery unavailable; original menu retained: " + e); }
            }
        });
    }

    private static void install(Context context, ClassLoader cl) throws Exception {
        System.loadLibrary("dexkit");
        try (DexKitBridge bridge = DexKitBridge.create(context.getApplicationInfo().sourceDir)) {
            List<ClassData> peers = bridge.findClass(FindClass.create().matcher(ClassMatcher.create()
                    .usingStrings("Overflow back button clicked", "RuntimeException: Failed to copy text to clipboard.")));
            if (peers.size() != 1) throw new IllegalStateException("Ambiguous Lens action-menu peer");
            Class<?> peer = peers.get(0).getInstance(cl);
            Method view = uniqueMethod(peer, method -> method.getParameterCount() == 0
                    && method.getReturnType().getName().equals("com.google.android.libraries.lens.view.actionmenu.ActionMenuView"));
            Method text = uniqueMethod(peer, method -> method.getParameterCount() == 0
                    && method.getReturnType() == String.class);
            List<MethodData> selects = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .paramCount(0).returnType("void")
                    .usingStrings("Select-all-text did not resolve to a selection")));
            if (selects.size() != 1) throw new IllegalStateException("Ambiguous Lens select-all action");
            Method select = selects.get(0).getMethodInstance(cl);
            List<Field> controllerFields = new ArrayList<>();
            List<Field> peerFields = new ArrayList<>();
            for (var used : selects.get(0).getUsingFields()) {
                Field field = used.getField().getFieldInstance(cl);
                if (Modifier.isStatic(field.getModifiers())
                        || field.getDeclaringClass() != select.getDeclaringClass()) continue;
                for (Field other : peer.getDeclaredFields()) {
                    if (!Modifier.isStatic(other.getModifiers()) && field.getType() == other.getType()) {
                        controllerFields.add(field); peerFields.add(other);
                    }
                }
            }
            if (controllerFields.size() != 1) throw new IllegalStateException("Ambiguous Lens overlay relationship");
            Field controllerOverlay = controllerFields.get(0), peerOverlay = peerFields.get(0);
            controllerOverlay.setAccessible(true); peerOverlay.setAccessible(true); select.setAccessible(true);
            mapping = new Mapping(select.getDeclaringClass(), view, text, select, peerOverlay, controllerOverlay);
        }
        List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
        try {
            hooks.addAll(XposedBridge.hookAllConstructors(mapping.controller(), new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam p) {
                    synchronized (controllers) {
                        controllers.removeIf(w -> w.get() == null);
                        controllers.add(new WeakReference<>(p.thisObject));
                    }
                }
            }));
            hooks.add(XposedBridge.hookMethod(mapping.view(), new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        if (p.hasThrowable() || !(p.getResult() instanceof View)) return;
                        View view = (View) p.getResult();
                        State state = states.get(view);
                        if (state == null) { state = new State(view); states.put(view, state); }
                        state.peer = new WeakReference<>(p.thisObject);
                        state.scheduleUpdate(view);
                    } catch (Throwable e) { XposedBridge.log("CircleNativeMenu update: " + e); }
                }
            }));
            XposedBridge.log("CircleNativeMenu: generic hooks installed " + android.os.Process.myPid());
        } catch (Throwable error) {
            for (XC_MethodHook.Unhook hook : hooks) hook.unhook();
            throw new IllegalStateException("Lens hooks unavailable; original menu retained", error);
        }
    }

    private interface MethodCheck { boolean matches(Method method); }
    private static Method uniqueMethod(Class<?> owner, MethodCheck check) {
        List<Method> matches = new ArrayList<>();
        for (Method method : owner.getDeclaredMethods())
            if (!Modifier.isStatic(method.getModifiers()) && check.matches(method)) matches.add(method);
        if (matches.size() != 1) throw new IllegalStateException("Ambiguous Lens accessor");
        Method method = matches.get(0); method.setAccessible(true); return method;
    }

    private static final class State extends ActionMode.Callback2 {
        final WeakReference<View> anchor;
        WeakReference<Object> peer;
        ActionMode mode;
        String selected = "", dismissed = "";
        volatile int classificationGeneration;
        String classifiedText = "";
        List<RemoteAction> smartActions = Collections.emptyList();
        final Map<Integer, RemoteAction> smartMenuActions = new HashMap<>();
        boolean updating;
        boolean updatePending;
        void scheduleUpdate(View view) {
            if (updatePending || updating) return;
            updatePending = true;
            view.post(() -> { updatePending = false; update(); });
        }
        State(View v) {
            anchor = new WeakReference<>(v);
            v.getViewTreeObserver().addOnGlobalLayoutListener(() -> update());
            v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                public void onViewAttachedToWindow(View v) {}
                public void onViewDetachedFromWindow(View v) { finish(); states.remove(v); }
            });
        }
        View buttons(View v) { return v.findViewById(v.getResources().getIdentifier("lens_action_menu_buttons", "id", GOOGLE)); }
        void finish() {
            classificationGeneration++;
            smartActions = Collections.emptyList(); classifiedText = ""; smartMenuActions.clear();
            if (mode != null) { ActionMode old = mode; mode = null; old.finish(); }
        }
        void classify(Context context, String text) {
            final int generation = ++classificationGeneration;
            smartActions = Collections.emptyList(); classifiedText = "";
            final Context app = context.getApplicationContext();
            final android.os.LocaleList locales = context.getResources().getConfiguration().getLocales();
            classificationExecutor.execute(() -> {
                TextClassifier classifier = null;
                try {
                    if (generation != classificationGeneration) return;
                    TextClassificationManager manager = app.getSystemService(TextClassificationManager.class);
                    if (manager == null) return;
                    classifier = manager.createTextClassificationSession(new TextClassificationContext.Builder(
                        GOOGLE, TextClassifier.WIDGET_TYPE_TEXTVIEW).build());
                    TextClassification result = classifier.classifyText(new TextClassification.Request.Builder(
                        text, 0, text.length()).setDefaultLocales(locales).build());
                    final List<RemoteAction> actions = new ArrayList<>(result.getActions());
                    View v = anchor.get();
                    if (v != null) v.post(() -> {
                        if (generation != classificationGeneration || !text.equals(selected) || !v.isAttachedToWindow()) return;
                        smartActions = actions; classifiedText = text;
                        XposedBridge.log("CircleNativeMenu: system classification returned " + actions.size() + " actions");
                        if (mode != null) mode.invalidate();
                    });
                } catch (Throwable e) { XposedBridge.log("CircleNativeMenu classifier: " + e.getClass().getSimpleName()); }
                finally { if (classifier != null) classifier.destroy(); }
            });
        }
        void update() {
            if (updating) return;
            updating = true;
            try {
                View v = anchor.get(); Object p = peer == null ? null : peer.get();
                if (v == null || p == null) return;
                View b = buttons(v);
                String text = (String) mapping.text().invoke(p);
                if (!v.isAttachedToWindow() || !v.isShown() || b == null || b.getVisibility() != View.VISIBLE || text.isEmpty()) {
                    finish(); selected = ""; dismissed = "";
                    if (b != null) { b.setAlpha(1f); b.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO); }
                    return;
                }
                boolean changed = !text.equals(selected); selected = text;
                if (changed) classify(v.getContext(), text);
                if (mode == null && !text.equals(dismissed)) {
                    mode = v.startActionMode(this, ActionMode.TYPE_FLOATING);
                    if (mode != null) XposedBridge.log("CircleNativeMenu: native toolbar shown");
                } else if (mode != null && changed) { mode.invalidate(); mode.invalidateContentRect(); }
                if (mode != null || text.equals(dismissed)) {
                    b.setAlpha(0f);
                    b.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                }
            } catch (Throwable e) { XposedBridge.log("CircleNativeMenu selection: " + e); }
            finally { updating = false; }
        }
        public boolean onCreateActionMode(ActionMode m, Menu menu) {
            menu.add(0, android.R.id.copy, 1, android.R.string.copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add(0, android.R.id.shareText, 2, "Share").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add(0, android.R.id.selectAll, 3, android.R.string.selectAll).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add(0, 10, 4, "Web search").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            return true;
        }
        public boolean onPrepareActionMode(ActionMode m, Menu menu) {
            menu.removeGroup(100);
            menu.removeGroup(101);
            smartMenuActions.clear();
            View v = anchor.get(); if (v == null) return false;
            Context c = v.getContext(); PackageManager pm = c.getPackageManager();
            Set<String> smartLabels = new HashSet<>();
            int smartIndex = 0;
            if (selected.equals(classifiedText)) for (RemoteAction action : smartActions) {
                if (!action.isEnabled()) continue;
                int id = smartIndex == 0 ? android.R.id.textAssist : 10000 + smartIndex;
                MenuItem item = menu.add(101, id, smartIndex == 0 ? 0 : 50 + smartIndex, action.getTitle());
                item.setShowAsAction(smartIndex == 0 ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
                if (action.shouldShowIcon()) try { item.setIcon(action.getIcon().loadDrawable(c)); } catch (RuntimeException ignored) {}
                smartMenuActions.put(id, action);
                smartLabels.add(action.getTitle().toString().trim().toLowerCase(Locale.ROOT));
                smartIndex++;
            }
            List<ResolveInfo> apps = pm.queryIntentActivities(new Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain"), 0);
            int i = 0;
            for (ResolveInfo app : apps) {
                ActivityInfo a = app.activityInfo;
                if (!a.exported && !GOOGLE.equals(a.packageName)) continue;
                if (a.permission != null && c.checkSelfPermission(a.permission) != PackageManager.PERMISSION_GRANTED) continue;
                if (smartLabels.contains(app.loadLabel(pm).toString().trim().toLowerCase(Locale.ROOT))) continue;
                Intent intent = new Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain")
                    .putExtra(Intent.EXTRA_PROCESS_TEXT, selected).putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                    .setClassName(a.packageName, a.name);
                menu.add(100, 1000 + i++, 100 + i, app.loadLabel(pm)).setIntent(intent).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            }
            return true;
        }
        public boolean onActionItemClicked(ActionMode m, MenuItem item) {
            View v = anchor.get(); Object p = peer.get(); if (v == null || p == null) return false;
            try {
                String text = (String) mapping.text().invoke(p);
                Context c = v.getContext(); int id = item.getItemId();
                RemoteAction smart = smartMenuActions.get(id);
                if (smart != null) {
                    if (!text.equals(classifiedText)) { update(); return false; }
                    if (!v.isShown()) return false;
                    ActivityOptions options = ActivityOptions.makeBasic();
                    if (android.os.Build.VERSION.SDK_INT >= 34) {
                        options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                    }
                    smart.getActionIntent().send(c, 0, null, null, null, null, options.toBundle());
                } else if (id == android.R.id.copy) {
                    ((ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Circle to Search", text));
                } else if (id == android.R.id.selectAll) {
                    Object overlay = mapping.peerOverlay().get(p);
                    synchronized (controllers) {
                        for (WeakReference<Object> ref : controllers) {
                            Object main = ref.get();
                            if (main != null && mapping.controllerOverlay().get(main) == overlay) {
                                mapping.selectAll().invoke(main); v.post(this::update); return true;
                            }
                        }
                    }
                    throw new IllegalStateException("Lens selection controller unavailable");
                } else if (id == android.R.id.shareText) {
                    c.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null));
                } else if (id == 10) {
                    c.startActivity(new Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, text));
                } else if (item.getIntent() != null) {
                    c.startActivity(new Intent(item.getIntent()).putExtra(Intent.EXTRA_PROCESS_TEXT, text));
                } else return false;
                m.finish(); return true;
            } catch (Throwable e) { XposedBridge.log("CircleNativeMenu action: " + e); return false; }
        }
        public void onDestroyActionMode(ActionMode m) { if (mode == m) mode = null; dismissed = selected; }
        public void onGetContentRect(ActionMode m, View v, Rect out) {
            View b = buttons(v);
            if (b != null) {
                int[] a = new int[2], pos = new int[2]; v.getLocationOnScreen(a); b.getLocationOnScreen(pos);
                out.set(pos[0] - a[0], pos[1] - a[1], pos[0] - a[0] + b.getWidth(), pos[1] - a[1] + Math.max(1, b.getHeight()));
            } else out.set(0, 0, v.getWidth(), 1);
        }
    }
}
