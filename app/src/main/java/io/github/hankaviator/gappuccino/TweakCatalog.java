package io.github.hankaviator.gappuccino;

import java.util.List;

/** Keys are shared with the preserved hook implementations. */
public final class TweakCatalog {
    public record Tweak(String key, String title, String description, boolean defaultEnabled) {}
    public record App(String packageName, String title, List<Tweak> tweaks) {}
    public static final List<App> APPS = withProfilePromotions(List.of(
        new App("com.google.android.apps.photos", "Google Photos", List.of(
            new Tweak("geo_intent_fix", "Open coordinates in maps", "Turn photo coordinates into map links with a visible pin.", true),
            new Tweak("reconcile_changes", "Reconcile device changes", "Apply recognized pending changes through Photos’ own review controls.", true),
            new Tweak("skip_trash_confirmation", "Skip trash confirmation", "Confirm Photos’ move-to-trash dialogs automatically.", true),
            new Tweak("photos_tcp", "Use TCP during mobile boost", "Disable QUIC for new connections while HyperOS mobile data boost is on. Photos backup restrictions still apply.", true))),
        new App("com.google.android.gm", "Gmail", List.of(
            new Tweak("gmail", "Hide sponsored emails", "Hide sponsored rows in the Promotions and Social inboxes.", true))),
        new App("com.google.android.apps.maps", "Google Maps", List.of(
            new Tweak("maps", "Hide sponsored offers", "Hide sponsored place cards and promotional offers.", true),
            new Tweak("maps_no_reverse_portrait", "Prevent upside-down portrait", "Keep normal portrait and landscape rotation.", true))),
        new App("com.google.android.dialer", "Google Phone", List.of(
            new Tweak("keep_current_app", "Stay in the current app", "Answer from notifications without opening Phone. Lock-screen and intentional call-screen launches stay visible.", true),
            new Tweak("enable_recording", "Enable call recording", "Enable Phone’s built-in recording eligibility.", true),
            new Tweak("disable_recording_disclosure", "Disable recording disclosure", "Silence the recording start and stop announcement audio. Detects compatible Phone implementations automatically. Restart Phone after changing this. Call behavior has not been tested.", false))),
        new App("com.google.android.googlequicksearchbox", "Google", List.of(
            new Tweak("circle_native_menu", "Native Circle to Search menu", "Use Android’s text-selection toolbar, including smart actions. Detects compatible Lens selection code automatically.", true))),
        new App("com.google.android.as", "Android System Intelligence", List.of(
            new Tweak("asi_multilingual", "Multilingual smart selection", "Use Android’s multilingual classifier when Google’s classifier finds no useful result.", true),
            new Tweak("asi_smart_reply", "Smart Reply providers", "Set up Google’s conversation capture and reply providers. Requires root and a reboot. Keeps your password autofill provider. Suggestions depend on supported apps and available models.", false),
            new Tweak("asi_smart_reply_locale", "Replies with a different display language", "Allow supported-language conversations to use English models when your phone’s display language is unsupported. Requires Smart Reply providers. Does not add Chinese reply models. Supports ASI V41.", true),
            new Tweak("asi_telegram_capture", "Experimental official Telegram capture", "Add official Telegram to System Intelligence’s capture and reply allowlists for compatibility testing. This does not supply conversation parsing or reply models. Requires Smart Reply providers and ASI V41.", false),
            new Tweak("asi_smart_reply_trace", "Trace Smart Reply diagnostics", "Log capture and reply-request counts for testing. Conversation text and replies are not logged. Restart System Intelligence after changing this.", false))),
        new App("com.android.vending", "Google Play Store", List.of(
            new Tweak("playstore_skip_disabled", "Skip apps with auto-update off", "Skip their approval screens during Update all. Detects compatible approval code automatically.", true),
            new Tweak("playstore_self_update_lock", "Block Play Store self-updates", "Prevent Play Store from replacing itself; other app updates continue normally.", false)))
    ));
    public static final String GENERIC = "gappuccino.generic";
    public static final String PROFILE_AI = "profile_ai";
    public static final java.util.Set<String> PROFILE_PACKAGES = java.util.Set.of(
        "com.google.android.apps.photos",
        "com.google.android.gm",
        "com.google.android.apps.maps",
        "com.google.android.dialer",
        "com.google.android.googlequicksearchbox",
        "com.android.vending",
        "com.android.chrome",
        "com.google.android.apps.books",
        "com.google.android.apps.docs",
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.googlevoice",
        "com.google.android.apps.magazines",
        "com.google.android.apps.messaging",
        "com.google.android.apps.walletnfcrel",
        "com.google.android.calendar",
        "com.google.android.contacts",
        "com.google.android.youtube",
        "com.google.android.apps.adm",
        "com.google.android.apps.bard",
        "com.google.android.apps.chromecast.app",
        "com.google.android.apps.dynamite",
        "com.google.android.apps.fitness",
        "com.google.android.apps.tasks",
        "com.google.android.apps.translate",
        "com.google.android.keep",
        "com.google.ar.lens",
        "com.niksoftware.snapseed",
        "com.google.android.inputmethod.latin");
    public static boolean isHookTarget(String packageName) {
        return PROFILE_PACKAGES.contains(packageName) || "com.google.android.as".equals(packageName);
    }
    /** Preserve a prior opt-out when replacing per-app switches with one shared switch. */
    static boolean profileAiEnabled(java.util.Map<String, ?> values) {
        Object shared = values.get(PROFILE_AI);
        if (shared instanceof Boolean) return (Boolean) shared;
        for (String pkg : PROFILE_PACKAGES)
            if (Boolean.FALSE.equals(values.get("profile_ai_" + pkg))) return false;
        return true;
    }
    private static List<App> withProfilePromotions(List<App> existing) {
        java.util.ArrayList<App> result = new java.util.ArrayList<>();
        result.add(new App(GENERIC, "Generic", List.of(new Tweak(PROFILE_AI,
                "Hide Google AI plan promotion",
                "Hide the Google AI plan offer in compatible profile menus across all covered Google apps. Account settings and subscriptions stay available.", true))));
        for (App app : existing) {
            java.util.ArrayList<Tweak> tweaks = new java.util.ArrayList<>(app.tweaks());
            if (app.packageName().equals("com.android.vending"))
                tweaks.add(new Tweak("playstore_profile_points", "Hide Play Points invitation",
                        "Hide the invitation to join Play Points in the profile menu. Existing membership and points controls stay available.", true));
            result.add(new App(app.packageName(), app.title(), List.copyOf(tweaks)));
        }
        return List.copyOf(result);
    }
    public static App find(String packageName) {
        for (App app : APPS) if (app.packageName().equals(packageName)) return app;
        return null;
    }
    public static boolean defaultEnabled(String key) {
        for (App app : APPS) for (Tweak tweak : app.tweaks())
            if (tweak.key().equals(key)) return tweak.defaultEnabled();
        throw new IllegalArgumentException("Unknown tweak: " + key);
    }
    private TweakCatalog() {}
}
