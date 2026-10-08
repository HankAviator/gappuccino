package io.github.hankaviator.gappuccino;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.ComponentActivity;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.divider.MaterialDivider;
import io.github.hankaviator.gdialertweak.testcall.TestCallManager;

/** Two levels: affected apps with their icons, then each app's tweak switches. */
public final class MainActivity extends ComponentActivity {
    private String selectedPackage;
    private LinearLayout root;
    private LinearLayout content;
    private NestedScrollView scroll;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FeatureSettings.ensureDefaults(this);
        prefs = FeatureSettings.open(this);
        if (state != null) selectedPackage = state.getString("selected_app");
        if (TweakCatalog.find(selectedPackage) == null) selectedPackage = null;
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (selectedPackage != null) showHome();
                else { setEnabled(false); getOnBackPressedDispatcher().onBackPressed(); }
            }
        });
        render();
        // App pages restore their identified scrolling views and app-bar behavior automatically.
        if (state != null && selectedPackage == null)
            scroll.post(() -> scroll.scrollTo(0, state.getInt("scroll_y", 0)));
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("selected_app", selectedPackage);
        state.putInt("scroll_y", scroll.getScrollY());
    }

    private void showHome() { selectedPackage = null; render(); }

    private void render() {
        root = vertical();
        int surface = color(com.google.android.material.R.attr.colorSurface);
        root.setBackgroundColor(surface);
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(getWindow(), root);
        boolean light = ColorUtils.calculateLuminance(surface) > .5;
        bars.setAppearanceLightStatusBars(light);
        bars.setAppearanceLightNavigationBars(light);

        TweakCatalog.App app = TweakCatalog.find(selectedPackage);
        if (app != null) {
            getLayoutInflater().inflate(R.layout.page_tweaks, root, true);
            MaterialToolbar toolbar = findViewById(R.id.page_toolbar);
            toolbar.setTitle(app.title());
            toolbar.setSubtitle(app.packageName());
            toolbar.setNavigationIconTint(color(com.google.android.material.R.attr.colorOnSurface));
            toolbar.setNavigationContentDescription("Back to apps");
            toolbar.setNavigationOnClickListener(v -> showHome());
            CollapsingToolbarLayout header = findViewById(R.id.flexible_header);
            header.setTitle(app.title());
            header.setSubtitle(app.packageName());
            header.setContentDescription(app.title() + ", " + app.packageName());
            scroll = findViewById(R.id.page_scroll);
            content = findViewById(R.id.page_content);
            renderApp(app);
            ViewCompat.requestApplyInsets(root);
            return;
        }
        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Gappuccino");
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(64)));
        scroll = new NestedScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content = vertical();
        content.setPadding(dp(20), dp(12), dp(20), dp(28));
        scroll.addView(content);
        renderHome();
        ViewCompat.requestApplyInsets(root);
    }

    private void renderHome() {
        TextView title = text("Your Google apps", 30, true);
        content.addView(title);
        TextView subtitle = text("Choose an app to customize its tweaks.", 16, false);
        subtitle.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        addWithGap(subtitle, 8);
        int width = getResources().getConfiguration().screenWidthDp;
        float fontScale = getResources().getConfiguration().fontScale;
        int columns = fontScale > 1.3f || width < 340 ? 1 : width >= 600 ? 3 : 2;
        for (int index = 0; index < TweakCatalog.APPS.size(); index += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < columns; col++) {
                int item = index + col;
                View card = item < TweakCatalog.APPS.size() ? appButton(TweakCatalog.APPS.get(item)) : new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
                if (col > 0) params.setMarginStart(dp(12));
                row.addView(card, params);
            }
            addWithGap(row, index == 0 ? 24 : 12);
        }
    }

    private MaterialCardView appButton(TweakCatalog.App app) {
        MaterialCardView card = card();
        LinearLayout body = vertical();
        body.setPadding(dp(18), dp(20), dp(18), dp(20));
        ImageView icon = icon(app);
        body.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView name = text(app.title(), 18, true);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(18);
        body.addView(name, nameParams);
        String summary = app.tweaks().size() + (app.tweaks().size() == 1 ? " tweak" : " tweaks");
        TextView count = text(summary, 14, false);
        count.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(-1, -2);
        countParams.topMargin = dp(6);
        body.addView(count, countParams);
        card.addView(body);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(app.title() + ", " + summary);
        // Expose each entire card as one app button to TalkBack.
        body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        card.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.Button");
            }
        });
        card.setOnClickListener(v -> { selectedPackage = app.packageName(); render(); });
        return card;
    }

    private void renderApp(TweakCatalog.App app) {
        TextView label = text("Tweaks", 14, false);
        label.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        label.setTextColor(color(androidx.appcompat.R.attr.colorPrimary));
        label.setPadding(dp(16), dp(12), dp(16), dp(8));
        ViewCompat.setAccessibilityHeading(label, true);
        content.addView(label);
        if (!isInstalled(app)) {
            TextView notice = text("This app is not installed. You can still configure its tweaks.", 14, false);
            notice.setPadding(dp(16), dp(8), dp(16), dp(16));
            content.addView(notice);
        }
        for (int i = 0; i < app.tweaks().size(); i++) {
            content.addView(tweakRow(app.tweaks().get(i)), new LinearLayout.LayoutParams(-1, -2));
            if (i < app.tweaks().size() - 1) {
                MaterialDivider divider = new MaterialDivider(this);
                divider.setDividerInsetStart(dp(16));
                divider.setDividerInsetEnd(dp(16));
                content.addView(divider, new LinearLayout.LayoutParams(-1, -2));
            }
        }
        TextView note = text("Restart " + app.title() + " after changing a tweak.", 14, false);
        note.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        note.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        note.setPadding(dp(16), dp(16), dp(16), dp(8));
        content.addView(note);
        if (app.packageName().equals("com.google.android.dialer")) renderTestCalls();
    }

    private LinearLayout tweakRow(TweakCatalog.Tweak tweak) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setMinimumHeight(dp(88));
        row.setBackgroundColor(color(com.google.android.material.R.attr.colorSurface));
        android.util.TypedValue ripple = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        row.setForeground(getDrawable(ripple.resourceId));
        LinearLayout labels = vertical();
        TextView headline = text(tweak.title(), 16, false);
        headline.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        labels.addView(headline, new LinearLayout.LayoutParams(-1, -2));
        TextView description = text(tweak.description(), 14, false);
        description.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        description.setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        LinearLayout.LayoutParams supporting = new LinearLayout.LayoutParams(-1, -2);
        supporting.topMargin = dp(4);
        labels.addView(description, supporting);
        // The row is one accessible switch, rather than separate text and control stops.
        labels.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        MaterialSwitch toggle = new MaterialSwitch(this);
        toggle.setMinWidth(dp(48));
        toggle.setMinHeight(dp(48));
        toggle.setChecked(prefs.getBoolean(tweak.key(), tweak.defaultEnabled()));
        toggle.setClickable(false);
        toggle.setFocusable(false);
        toggle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams trailing = new LinearLayout.LayoutParams(-2, dp(48));
        trailing.setMarginStart(dp(16));
        row.addView(toggle, trailing);
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(tweak.title() + ". " + tweak.description());
        row.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.Switch");
                info.setCheckable(true);
                info.setChecked(toggle.isChecked());
            }
        });
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            row.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
            if (!tweak.key().equals("asi_smart_reply")) {
                prefs.edit().putBoolean(tweak.key(), checked).apply();
                return;
            }
            toggle.setEnabled(false);
            row.setEnabled(false);
            new Thread(() -> {
                try {
                    String message = SmartReplySetup.configure(this, checked);
                    prefs.edit().putBoolean(tweak.key(), checked).commit();
                    runOnUiThread(() -> { toggle.setEnabled(true); row.setEnabled(true); Toast.makeText(this, message, Toast.LENGTH_LONG).show(); });
                } catch (Exception error) {
                    runOnUiThread(() -> { render();
                        Toast.makeText(this, "Root setup failed: " + error.getMessage(), Toast.LENGTH_LONG).show(); });
                }
            }, "SmartReplySetup").start();
        });
        return row;
    }

    private void renderTestCalls() {
        // Keep secondary tools visually separate from the settings list.
        LinearLayout list = content;
        content = vertical();
        content.setPadding(dp(16), 0, dp(16), 0);
        list.addView(content, new LinearLayout.LayoutParams(-1, -2));
        addWithGap(text("Test incoming calls", 20, true), 24);
        addWithGap(text("Use a local test call to check answering behavior without a carrier call.", 14, false), 8);
        MaterialButton account = new MaterialButton(this);
        account.setText("Enable test calling account");
        account.setOnClickListener(v -> {
            try { TestCallManager.register(this); startActivity(new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)); }
            catch (RuntimeException error) { Toast.makeText(this, "Could not open calling accounts", Toast.LENGTH_LONG).show(); }
        });
        addWithGap(account, 12);
        MaterialButton test = new MaterialButton(this);
        test.setText("Simulate incoming call");
        test.setOnClickListener(v -> {
            try { TestCallManager.startIncoming(this); }
            catch (RuntimeException error) { Toast.makeText(this, "Enable the Gappuccino test calling account first", Toast.LENGTH_LONG).show(); }
        });
        addWithGap(test, 4);
        content = list;
    }

    private boolean isInstalled(TweakCatalog.App app) {
        try { getPackageManager().getApplicationInfo(app.packageName(), 0); return true; }
        catch (PackageManager.NameNotFoundException ignored) { return false; }
    }
    private ImageView icon(TweakCatalog.App app) {
        ImageView image = new ImageView(this);
        Drawable drawable;
        try { drawable = getPackageManager().getApplicationIcon(app.packageName()); }
        catch (PackageManager.NameNotFoundException ignored) { drawable = getDrawable(R.drawable.ic_launcher); }
        image.setImageDrawable(drawable);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return image;
    }
    private MaterialCardView card() {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(26));
        card.setCardElevation(0);
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceContainerLow));
        return card;
    }
    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));
        if (bold) view.setTypeface(view.getTypeface(), Typeface.BOLD);
        return view;
    }
    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }
    private void addWithGap(View view, int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(gap);
        content.addView(view, params);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int color(int attribute) { return MaterialColors.getColor(this, attribute, Color.BLACK); }
}
