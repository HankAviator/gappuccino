package io.github.hankaviator.gappuccino;

import java.util.List;

/** Keys are shared with the preserved hook implementations. */
public final class TweakCatalog {
    public record Tweak(String key, String title, String description, boolean defaultEnabled) {}
    public record App(String packageName, String title, List<Tweak> tweaks) {}
    public static final List<App> APPS = List.of(
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
    );
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
