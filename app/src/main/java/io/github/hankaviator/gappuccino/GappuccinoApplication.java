package io.github.hankaviator.gappuccino;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public final class GappuccinoApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
