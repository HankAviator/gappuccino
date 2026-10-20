package io.github.hankaviator.gdialertweak;

import io.github.hankaviator.gappuccino.runtime.*;


import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.telecom.TelecomManager;

import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class HookEntry implements FeatureInstaller {
    private static final String DIALER = "com.google.android.dialer";
    private static final String RECEIVER = "com.android.dialer.incall.statusbarnotification.buttonintent.impl.NotificationBroadcastReceiver_Receiver";
    private static final String INCALL_ACTIVITY = "com.android.dialer.incall.activity.ui.InCallActivity";
    private static final String ACTION_EXTRA = "com.android.dialer.incall.statusbarnotification.buttonintentActionValue";
    private static final String EXTRA_SHOW_DIALPAD = "InCallActivity.show_dialpad";
    private static final String EXTRA_NEW_OUTGOING = "InCallActivity.new_outgoing_call";
    private static final String EXTRA_FULL_SCREEN = "InCallActivity.for_full_screen";
    private static final String EXTRA_NOTIFICATION_BUTTON = "InCallActivity.for_notification_button";
    private static final String EXTRA_NOTIFICATION_CONTENT = "InCallActivity.for_notification_content";
    private static final String EXTRA_MODULE_NOTIFICATION_CONTENT =
            "io.github.hankaviator.gdialertweak.NOTIFICATION_CONTENT";
    private static final InCallLaunchPolicy uiPolicy = new InCallLaunchPolicy();
    private static final Set<Activity> notificationAnswerActivities =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static HookPreferences preferences;
    private static Mapping mapping;
    private static boolean installed;

    private record Mapping(String buttonIntents, String pendingIntents, String pendingMethod,
                           String canRecord, String recordingEnabled) {}

    @Override public void install(TargetPackage load) {
        if (!DIALER.equals(load.packageName)) return;
        preferences = new HookPreferences("io.github.hankaviator.gappuccino", Prefs.FILE);
        preferences.reload();
        log("remote preferences"
                + ", keepCurrentApp=" + preferences.getBoolean(Prefs.KEEP_APP, true)
                + ", recording=" + preferences.getBoolean(Prefs.RECORDING, true));

        Reflection.findAndHookMethod(Application.class, "attach", Context.class,
                new Callback() {
                    @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (installed) return;
                        installed = true;
                        Context context = (Context) param.args[0];
                        String version = context.getPackageManager().getPackageInfo(DIALER, 0).versionName;
                        if ("236.0.969488611-pixel".equals(version)) {
                            mapping = new Mapping("rcb", "aghx", "a", "kxt", "kwz");
                        } else if ("240.0.986973448-pixel".equals(version)) {
                            mapping = new Mapping("rve", "ahdi", "b", "ldt", "lcy");
                        }
                        if (mapping == null) {
                            log("version-specific hooks unavailable for Phone " + version);
                        }
                        hookNotificationPendingIntents();
                        hookNotificationAnswer(load.classLoader);
                        hookInternalInCallActivityLaunch(load.classLoader);
                        hookInCallActivity(load.classLoader);
                        hookRecordingEligibility(load.classLoader);
                        if (enabled(Prefs.SILENT_DISCLOSURE, false)) {
                            RecordingDisclosureHook.install(context, load.classLoader,
                                    () -> enabled(Prefs.SILENT_DISCLOSURE, false));
                        }
                        log("loaded in Google Phone " + version + " / " + load.processName);
                    }
                });
    }

    private static void hookNotificationPendingIntents() {
        // Framework factories are stable across Phone's obfuscated implementations.
        HookRuntime.hookAllMethods(PendingIntent.class, "getActivity", new Callback() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args.length < 4 || !(param.args[0] instanceof Context)
                        || !(param.args[2] instanceof Intent)) return;
                Intent intent = (Intent) param.args[2];
                ComponentName component = intent.getComponent();
                if (component == null || !DIALER.equals(component.getPackageName())
                        || !INCALL_ACTIVITY.equals(component.getClassName())) return;
                if (isAnswerAction(intent)) {
                    Context context = (Context) param.args[0];
                    if (!enabled(Prefs.KEEP_APP, true) || shouldShowInCallUi(context)) return;
                    Intent receiver = new Intent(intent).setComponent(new ComponentName(DIALER, RECEIVER));
                    param.setResult(PendingIntent.getBroadcast(context, (Integer) param.args[1],
                            receiver, (Integer) param.args[3]));
                    log("using receiver-only notification answer via framework factory");
                } else if (Intent.ACTION_MAIN.equals(intent.getAction())
                        && !intent.getBooleanExtra(EXTRA_NOTIFICATION_BUTTON, false)) {
                    intent.putExtra(EXTRA_MODULE_NOTIFICATION_CONTENT, true);
                }
            }
        });
        log("version-independent notification pending-intent hook installed");
    }

    private static boolean enabled(String key, boolean defaultValue) {
        preferences.reload();
        return preferences.getBoolean(key, defaultValue);
    }

    private static void hookNotificationAnswer(ClassLoader loader) {
        try {
            Class<?> receiver = Reflection.findClass(RECEIVER, loader);
            HookRuntime.hookMethod(receiver.getMethod("onReceive", Context.class, Intent.class), new Callback() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!RECEIVER.equals(param.thisObject.getClass().getName())
                            || !enabled(Prefs.KEEP_APP, true)) return;
                    Context context = (Context) param.args[0];
                    Intent intent = (Intent) param.args[1];
                    if (!isAnswerAction(intent) || shouldShowInCallUi(context)) return;
                    uiPolicy.answered(SystemClock.elapsedRealtime());
                    log("receiver answer detected; internal in-call activity launches will be blocked");
                }
            });
        } catch (Throwable error) {
            log("notification hook unavailable", error);
        }
    }

    private static boolean isAnswerAction(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.startsWith("com.android.dialer.incall.statusbarnotification.buttonintent")
                && action.contains("ANSWER")) return true;
        int value = intent.getIntExtra(ACTION_EXTRA, 0);
        return value == 1 || value == 2 || value == 6 || value == 7
                || value == 16 || value == 18 || value == 19;
    }

    private static boolean shouldShowInCallUi(Context context) {
        KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
        if (keyguard != null && keyguard.isKeyguardLocked()) return true;

        // Notification Answer is already an unambiguous user action. Foreground
        // task access is restricted on newer HyperOS and is not needed here.
        return false;
    }

    private static void hookInternalInCallActivityLaunch(ClassLoader loader) {
        try {
            Class<?> instrumentation = Reflection.findClass("android.app.Instrumentation", loader);
            HookRuntime.hookAllMethods(instrumentation, "execStartActivity", new Callback() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!uiPolicy.suppress(SystemClock.elapsedRealtime()) || !enabled(Prefs.KEEP_APP, true)) return;
                    for (Object argument : param.args) {
                        if (!(argument instanceof Intent)) continue;
                        Intent intent = (Intent) argument;
                        ComponentName component = intent.getComponent();
                        if (component != null && DIALER.equals(component.getPackageName())
                                && INCALL_ACTIVITY.equals(component.getClassName())
                                && !isExplicitUiLaunch(intent)) {
                            param.setResult(null);
                            log("blocked Google Phone's internal InCallActivity launch after receiver answer");
                            return;
                        }
                    }
                }
            });
            log("internal InCallActivity launch blocker installed");
        } catch (Throwable error) {
            log("internal InCallActivity launch blocker unavailable", error);
        }
    }

    private static void hookInCallActivity(ClassLoader loader) {
        try {
            Class<?> activityClass = Reflection.findClass(INCALL_ACTIVITY, loader);
            Callback rememberAnswerLaunch = new Callback() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    Intent intent = param.args.length > 0 && param.args[0] instanceof Intent
                            ? (Intent) param.args[0] : activity.getIntent();
                    if (isExplicitUiLaunch(intent)) {
                        notificationAnswerActivities.remove(activity);
                        uiPolicy.explicitOpen();
                        if (param.args.length > 0 && param.args[0] instanceof Intent)
                            activity.setIntent(intent);
                        log("explicit in-call UI launch; answer suppression cleared");
                        return;
                    }
                    if (isNotificationAnswerLaunch(intent)) {
                        notificationAnswerActivities.add(activity);
                        uiPolicy.answered(SystemClock.elapsedRealtime());
                        log("notification answer activity launch detected");
                    }
                }

                @Override protected void afterHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    if (uiPolicy.suppress(SystemClock.elapsedRealtime())) {
                        hardSuppressActivity(activity, "activity launch");
                    }
                }
            };
            HookRuntime.hookAllMethods(activityClass, "onCreate", rememberAnswerLaunch);
            HookRuntime.hookAllMethods(activityClass, "onNewIntent", rememberAnswerLaunch);
            HookRuntime.hookAllMethods(activityClass, "onResume", new Callback() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    if (!enabled(Prefs.KEEP_APP, true)) return;

                    Intent intent = activity.getIntent();
                    boolean explicitlyMarked = uiPolicy.suppress(SystemClock.elapsedRealtime());
                    boolean answerLaunch = notificationAnswerActivities.contains(activity)
                            || isNotificationAnswerLaunch(intent);
                    boolean fullScreen = intent != null && intent.getBooleanExtra(EXTRA_FULL_SCREEN, false);
                    boolean outgoing = intent != null && intent.getBooleanExtra(EXTRA_NEW_OUTGOING, false);
                    boolean notificationContent = intent != null
                            && (intent.getBooleanExtra(EXTRA_NOTIFICATION_CONTENT, false)
                            || intent.getBooleanExtra(EXTRA_MODULE_NOTIFICATION_CONTENT, false));
                    boolean notificationButton = intent != null
                            && intent.getBooleanExtra(EXTRA_NOTIFICATION_BUTTON, false);
                    boolean showDialpad = intent != null && intent.getBooleanExtra(EXTRA_SHOW_DIALPAD, false);

                    log("in-call resume: answerLaunch=" + answerLaunch
                            + ", marked=" + explicitlyMarked
                            + ", fullScreen=" + fullScreen
                            + ", outgoing=" + outgoing
                            + ", notificationButton=" + notificationButton
                            + ", notificationContent=" + notificationContent
                            + ", showDialpad=" + showDialpad);

                    // Full-screen/lock-screen calls and explicit notification-content taps should
                    // remain visible. Android 15's CallStyle Answer action bypasses the Dialer
                    // receiver and normally arrives here with no launch marker (or button=true).
                    if (isKeyguardLocked(activity) || fullScreen || outgoing || notificationContent) {
                        notificationAnswerActivities.remove(activity);
                        uiPolicy.explicitOpen();
                        return;
                    }
                    if (!explicitlyMarked) return;
                    hardSuppressActivity(activity, "resume");
                }
            });
            HookRuntime.hookAllMethods(activityClass, "onWindowFocusChanged", new Callback() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (Boolean.TRUE.equals(param.args[0]) && uiPolicy.suppress(SystemClock.elapsedRealtime())) {
                        hardSuppressActivity((Activity) param.thisObject, "window focus");
                    }
                }
            });
        } catch (Throwable error) {
            log("in-call activity hook unavailable", error);
        }
    }

    private static boolean isNotificationAnswerLaunch(Intent intent) {
        if (intent == null) return false;
        String action = intent.getAction();
        return (action != null && action.startsWith(
                "com.android.dialer.incall.statusbarnotification.buttonintentANSWER"))
                || (intent.getBooleanExtra(EXTRA_NOTIFICATION_BUTTON, false) && isAnswerAction(intent));
    }

    private static boolean isExplicitUiLaunch(Intent intent) {
        return intent != null && (intent.getBooleanExtra(EXTRA_FULL_SCREEN, false)
                || intent.getBooleanExtra(EXTRA_NEW_OUTGOING, false)
                || intent.getBooleanExtra(EXTRA_NOTIFICATION_CONTENT, false)
                || intent.getBooleanExtra(EXTRA_MODULE_NOTIFICATION_CONTENT, false));
    }

    private static void hardSuppressActivity(Activity activity, String phase) {
        if (!enabled(Prefs.KEEP_APP, true) || isKeyguardLocked(activity)) {
            notificationAnswerActivities.remove(activity);
            uiPolicy.explicitOpen();
            return;
        }
        if (!activity.isFinishing() && activity.moveTaskToBack(true)) {
            activity.overridePendingTransition(0, 0);
            log("backgrounded automatic answer UI during " + phase);
        }
    }

    private static boolean isKeyguardLocked(Context context) {
        KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
        return keyguard != null && keyguard.isKeyguardLocked();
    }

    private static void hookRecordingEligibility(ClassLoader loader) {
        Callback forceTrue = new Callback() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (enabled(Prefs.RECORDING, true)) param.setResult(true);
            }
        };
        if (mapping != null) {
            hookBooleanNoArg(loader, mapping.canRecord(), "a", forceTrue);
            hookBooleanNoArg(loader, mapping.recordingEnabled(), "a", forceTrue);
        }

        try {
            HookRuntime.hookAllMethods(
                    Reflection.findClass("android.app.ApplicationPackageManager", loader),
                    "hasSystemFeature", new Callback() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            if (enabled(Prefs.RECORDING, true) && param.args.length > 0
                                    && "com.google.android.apps.dialer.call_recording_audio".equals(param.args[0])) {
                                param.setResult(true);
                            }
                        }
                    });
        } catch (Throwable error) {
            log("recording system-feature hook unavailable", error);
        }
    }

    private static void hookBooleanNoArg(ClassLoader loader, String className, String methodName,
                                         Callback callback) {
        try {
            Class<?> type = Reflection.findClass(className, loader);
            if (type.getDeclaredMethod(methodName).getReturnType() != boolean.class) {
                throw new NoSuchMethodException("Expected a boolean eligibility method");
            }
            Reflection.findAndHookMethod(type, methodName, callback);
            log("recording gate hooked: " + className + "." + methodName);
        } catch (Throwable error) {
            log("recording gate unavailable: " + className + "." + methodName, error);
        }
    }

    private static void log(String message) {
        HookRuntime.log("GDialerTweak: " + message);
    }

    private static void log(String message, Throwable error) {
        HookRuntime.log("GDialerTweak: " + message + ": " + error);
    }
}
