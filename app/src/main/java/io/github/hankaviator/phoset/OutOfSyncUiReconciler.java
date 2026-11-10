package io.github.hankaviator.phoset;

import io.github.hankaviator.gappuccino.runtime.*;

import io.github.hankaviator.gappuccino.BuildConfig;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Uses Photos' own review controls, identified by stable resource names. */
final class OutOfSyncUiReconciler {
    private static final String TAG = "PhoSetSync";
    private static final String PHOTOS = "com.google.android.apps.photos";
    private static final long REVIEW_WINDOW_MS = 5 * 60_000L;
    private static final Map<View, Boolean> SCHEDULED_CHIPS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> VISIBILITY_FALLBACK =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Boolean> CLICKED_BUTTONS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Activity, Boolean> WATCHING_REVIEWS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile long automaticReviewStarted;
    private static volatile int actionsThisReview;
    private static WeakReference<View> pendingCard = new WeakReference<>(null);
    private static WeakReference<View> automaticChip = new WeakReference<>(null);

    private static final String[][] CARDS = {
            {"photos_outofsync_ui_edited_title",
                    "photos_outofsync_ui_edited_change_all_button_text"},
            {"photos_outofsync_ui_trashed_title",
                    "photos_outofsync_ui_trash_all_button_text"},
            {"photos_outofsync_ui_restored_title",
                    "photos_outofsync_ui_restore_all_button_text"},
            {"photos_outofsync_ui_deleted_title",
                    "photos_outofsync_ui_delete_all_button_text"},
            {"photos_outofsync_ui_vaulted_title",
                    "photos_outofsync_ui_vault_all_button_text"}
    };

    private OutOfSyncUiReconciler() {}

    static void install() {
        AutomaticReviewWindow.install();
        Reflection.findAndHookMethod(View.class, "setVisibility", int.class,
                new Callback() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        View view = (View) param.thisObject;
                        if ((int) param.args[0] != View.VISIBLE
                                || !isReviewChip(view)
                                || !FeatureFlags.isEnabled(FeatureFlags.RECONCILE_CHANGES)
                                || VISIBILITY_FALLBACK.containsKey(view)) {
                            return;
                        }
                        if (SCHEDULED_CHIPS.put(view, true) == null) {
                            // The click listener is already installed when Photos shows the chip.
                            view.postDelayed(() -> openReview(view), 250L);
                        }
                        param.args[0] = View.GONE;
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        View view = (View) param.thisObject;
                        if ((int) param.args[0] == View.VISIBLE
                                && hasResourceName(view, "empty_page_title_bottom")) {
                            view.postDelayed(() -> finishEmptyAutomaticReview(view), 250L);
                        }
                    }
                });
        Reflection.findAndHookMethod(View.class, "onAttachedToWindow",
                new Callback() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        View view = (View) param.thisObject;
                        if (!hasResourceName(view, "card_handle_all_button")
                                && !hasResourceName(view, "empty_page_title_bottom")
                                && !hasResourceName(view, "og_bento_card_title")) {
                            return;
                        }
                        if (!FeatureFlags.isEnabled(FeatureFlags.RECONCILE_CHANGES)) {
                            return;
                        }
                        if (hasResourceName(view, "og_bento_card_title")) {
                            view.postDelayed(() -> openProfileReview(view), 250L);
                        } else if (hasResourceName(view, "card_handle_all_button")) {
                            view.postDelayed(() -> processCard(view), 500L);
                        } else if (hasResourceName(view, "empty_page_title_bottom")) {
                            view.postDelayed(() -> finishEmptyAutomaticReview(view), 1500L);
                        }
                    }
                });
        HookRuntime.log(TAG + ": resource-based review controls installed ("
                + BuildConfig.VERSION_NAME + ")");
    }

    private static void openProfileReview(View title) {
        if (!FeatureFlags.isEnabled(FeatureFlags.RECONCILE_CHANGES)
                || !title.isAttachedToWindow() || !(title instanceof TextView text)) return;
        String expected = resourceString(title.getResources(),
                "photos_outofsync_strings_review_out_of_sync_text");
        if (expected == null || !TextUtils.equals(text.getText(), expected)) return;
        View current = title;
        for (int depth = 0; depth < 6; depth++) {
            if (current.isClickable() && current.hasOnClickListeners()) {
                if (SCHEDULED_CHIPS.put(current, true) == null) openReview(current);
                return;
            }
            if (!(current.getParent() instanceof View parent)) return;
            current = parent;
        }
    }

    private static boolean isReviewChip(View view) {
        if (!(view instanceof TextView)
                || !"Chip".equals(view.getClass().getSimpleName())) {
            return false;
        }
        String expected = resourceString(view.getResources(),
                "photos_outofsync_strings_review_out_of_sync_text");
        return expected != null && TextUtils.equals(((TextView) view).getText(), expected);
    }

    private static void openReview(View chip) {
        if (!FeatureFlags.isEnabled(FeatureFlags.RECONCILE_CHANGES)) {
            restoreChip(chip);
            return;
        }
        if (!chip.isAttachedToWindow() || !chip.hasOnClickListeners()) {
            restoreChip(chip);
            return;
        }
        automaticReviewStarted = SystemClock.elapsedRealtime();
        actionsThisReview = 0;
        automaticChip = new WeakReference<>(chip);
        if (!AutomaticReviewWindow.clickChip(chip)) {
            automaticReviewStarted = 0L;
            restoreChip(chip);
        }
    }

    private static void restoreChip(View chip) {
        SCHEDULED_CHIPS.remove(chip);
        VISIBILITY_FALLBACK.put(chip, true);
        chip.setVisibility(View.VISIBLE);
        log("review chip retained because its action was unavailable");
    }

    private static void processCard(View button) {
        if (!FeatureFlags.isEnabled(FeatureFlags.RECONCILE_CHANGES)
                || !button.isAttachedToWindow() || !button.isEnabled()
                || CLICKED_BUTTONS.containsKey(button)) {
            return;
        }
        Activity activity = activityFrom(button.getContext());
        if (activity == null || !activity.getClass().getName().contains(".outofsync.ui.")) {
            return;
        }
        if (!AutomaticReviewWindow.canFinish(activity)) return;
        View pending = pendingCard.get();
        if (pending != null && pending.isAttachedToWindow() && pending.isShown()) return;
        pendingCard.clear();
        View card = findCard(button);
        if (card == null) {
            log("review card structure changed; action left for manual review");
            AutomaticReviewWindow.leaveForManualReview(activity, "review card structure changed");
            return;
        }
        TextView title = card.findViewById(resourceId(button, "card_title"));
        TextView message = card.findViewById(resourceId(button, "card_message"));
        if (title == null || message == null || !(button instanceof TextView)
                || !isKnownCard(title, (TextView) button)) {
            log("unknown review card; action left for manual review");
            AutomaticReviewWindow.leaveForManualReview(activity, "unknown review card");
            return;
        }
        long count = ReviewCountParser.firstCount(message.getText());
        if (count < 1) {
            log("review card count is missing or invalid: " + count);
            AutomaticReviewWindow.leaveForManualReview(activity, "review count unavailable");
            return;
        }
        // An automatically handled review can also be opened from another Photos entry point.
        if (automaticReviewStarted == 0L
                || SystemClock.elapsedRealtime() - automaticReviewStarted > REVIEW_WINDOW_MS) {
            automaticReviewStarted = SystemClock.elapsedRealtime();
            actionsThisReview = 0;
        }
        if (actionsThisReview >= CARDS.length) {
            log("review action limit reached; remaining cards left for manual review");
            AutomaticReviewWindow.leaveForManualReview(activity, "review action limit reached");
            return;
        }
        pendingCard = new WeakReference<>(card);
        CLICKED_BUTTONS.put(button, true);
        actionsThisReview++;
        log("applying Photos review action: " + title.getText() + " (" + count + ")");
        if (!button.performClick()) {
            pendingCard.clear();
            CLICKED_BUTTONS.remove(button);
            actionsThisReview--;
            log("Photos review button did not accept the click");
            AutomaticReviewWindow.leaveForManualReview(activity, "Photos did not accept the action");
        } else {
            // Photos may reuse its empty-state view after the media permission flow.
            // Keep checking for the completed state rather than relying on attachment.
            watchForCompletedReview(activity);
        }
    }

    private static View findCard(View button) {
        View current = button;
        for (int depth = 0; depth < 6; depth++) {
            ViewParent parent = current.getParent();
            if (!(parent instanceof View)) {
                return null;
            }
            current = (View) parent;
            int titleId = resourceId(button, "card_title");
            int messageId = resourceId(button, "card_message");
            if (titleId != 0 && messageId != 0
                    && current.findViewById(titleId) != null
                    && current.findViewById(messageId) != null) {
                return current;
            }
        }
        return null;
    }

    private static boolean isKnownCard(TextView title, TextView button) {
        Resources resources = title.getResources();
        for (String[] card : CARDS) {
            String expectedTitle = resourceString(resources, card[0]);
            String expectedAction = resourceString(resources, card[1]);
            if (expectedTitle != null && expectedAction != null
                    && TextUtils.equals(title.getText(), expectedTitle)
                    && TextUtils.equals(button.getText(), expectedAction)) {
                return true;
            }
        }
        return false;
    }

    private static void finishEmptyAutomaticReview(View emptyTitle) {
        long started = automaticReviewStarted;
        Activity activity = activityFrom(emptyTitle.getContext());
        boolean hidden = activity != null && AutomaticReviewWindow.isHidden(activity);
        if (started == 0L || (actionsThisReview == 0 && !hidden)
                || (actionsThisReview == 0 && SystemClock.elapsedRealtime() - started < 5000L)
                || SystemClock.elapsedRealtime() - started > REVIEW_WINDOW_MS
                || !emptyTitle.isShown()) {
            return;
        }
        if (activity != null && AutomaticReviewWindow.canFinish(activity)
                && activity.getClass().getName().contains(".outofsync.ui.")) {
            automaticReviewStarted = 0L;
            log("Photos review completed; closing automatic review");
            activity.finish();
            activity.overridePendingTransition(0, 0);
        }
    }

    static void onAutomaticReviewCreated() {
        automaticReviewStarted = SystemClock.elapsedRealtime();
        actionsThisReview = 0;
        pendingCard.clear();
        CLICKED_BUTTONS.clear();
    }

    static void onAutomaticReviewAbandoned() {
        automaticReviewStarted = 0L;
        actionsThisReview = 0;
        pendingCard.clear();
        CLICKED_BUTTONS.clear();
        View chip = automaticChip.get();
        automaticChip.clear();
        if (chip != null) {
            chip.postDelayed(() -> restoreChip(chip), 250L);
        }
    }

    static void watchForCompletedReview(Activity activity) {
        if (WATCHING_REVIEWS.put(activity, true) == null) checkReview(activity);
    }

    private static void checkReview(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()
                || automaticReviewStarted == 0L
                || SystemClock.elapsedRealtime() - automaticReviewStarted > REVIEW_WINDOW_MS) {
            WATCHING_REVIEWS.remove(activity);
            return;
        }
        View decor = activity.getWindow().getDecorView();
        int emptyTitleId = resourceId(decor, "empty_page_title_bottom");
        View emptyTitle = emptyTitleId == 0 ? null : decor.findViewById(emptyTitleId);
        if (emptyTitle != null && emptyTitle.isShown() && AutomaticReviewWindow.canFinish(activity)) {
            finishEmptyAutomaticReview(emptyTitle);
            if (automaticReviewStarted == 0L) {
                WATCHING_REVIEWS.remove(activity);
                return;
            }
        }
        // A previous native action can detach its card without attaching a new
        // button. Continue with one remaining card only after Photos resumes.
        int buttonId = resourceId(decor, "card_handle_all_button");
        View nextButton = buttonId == 0 ? null : findPendingButton(decor, buttonId);
        if (nextButton != null) processCard(nextButton);
        decor.postDelayed(() -> checkReview(activity), 500L);
    }

    private static View findPendingButton(View view, int id) {
        if (!view.isShown()) return null;
        if (view.getId() == id && view.isEnabled() && !CLICKED_BUTTONS.containsKey(view)) return view;
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View button = findPendingButton(group.getChildAt(i), id);
                if (button != null) return button;
            }
        }
        return null;
    }

    private static Activity activityFrom(Context context) {
        for (int depth = 0; context instanceof ContextWrapper && depth < 10; depth++) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private static int resourceId(View view, String name) {
        return view.getResources().getIdentifier(name, "id", PHOTOS);
    }

    private static String resourceString(Resources resources, String name) {
        int id = resources.getIdentifier(name, "string", PHOTOS);
        return id == 0 ? null : resources.getString(id);
    }

    private static boolean hasResourceName(View view, String name) {
        int id = view.getId();
        if (id == 0 || id == View.NO_ID) {
            return false;
        }
        try {
            return name.equals(view.getResources().getResourceEntryName(id));
        } catch (Resources.NotFoundException ignored) {
            return false;
        }
    }

    static void log(String message) {
        Log.i(TAG, message);
        HookRuntime.log(TAG + ": " + message);
    }
}
