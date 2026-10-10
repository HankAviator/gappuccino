package io.github.hankaviator.gappuccino;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

public final class FeatureSettings {
    public static final String FILE = "features";
    private FeatureSettings() {}
    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    public static SharedPreferences open(Context context) {
        try { return context.getSharedPreferences(FILE, Context.MODE_WORLD_READABLE); }
        catch (SecurityException ignored) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    }
    public static void ensureDefaults(Context context) {
        SharedPreferences prefs = open(context);
        SharedPreferences.Editor edit = prefs.edit();
        if (!prefs.contains(TweakCatalog.PROFILE_AI))
            edit.putBoolean(TweakCatalog.PROFILE_AI, TweakCatalog.profileAiEnabled(prefs.getAll()));
        for (TweakCatalog.App app : TweakCatalog.APPS) for (TweakCatalog.Tweak tweak : app.tweaks())
            if (!tweak.key().equals(TweakCatalog.PROFILE_AI) && !prefs.contains(tweak.key())) edit.putBoolean(tweak.key(), tweak.defaultEnabled());
        edit.apply();
    }
}
