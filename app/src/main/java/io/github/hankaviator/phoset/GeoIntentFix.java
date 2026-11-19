package io.github.hankaviator.phoset;

import io.github.hankaviator.gappuccino.runtime.*;


import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

final class GeoIntentFix {
    private GeoIntentFix() {}

    static void install() {
        HookRuntime.hookAllMethods(Instrumentation.class, "execStartActivity", new Callback() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!FeatureFlags.isEnabled(FeatureFlags.GEO_INTENT_FIX)) {
                    return;
                }
                for (int index = 0; index < param.args.length; index++) {
                    if (param.args[index] instanceof Intent) {
                        Intent original = (Intent) param.args[index];
                        Context context = param.args[0] instanceof Context
                                ? (Context) param.args[0] : null;
                        Intent rewritten = IntentRewriter.rewrite(original, context);
                        if (rewritten != original) {
                            param.args[index] = rewritten;
                        }
                        return;
                    }
                }
            }
        });
    }
}
