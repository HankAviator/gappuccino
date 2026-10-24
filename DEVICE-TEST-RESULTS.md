# Device test results

## Chrome file-type confirmation (0.2.3)

October 21, 2026. Added Google Chrome's default-on **Skip harmful-file
confirmation** switch. It supplies Chrome's own acceptance callback for the
“File might be harmful” dialog. The hook does not change Safe Browsing settings,
malware-recovery dialogs, enterprise warnings, duplicate-file prompts or install
permissions. In newer shared dialogs it requires the dangerous-file flag.

The installed Chrome 155.0.8059.84 APKs were inspected offline. Generic discovery
found the optimized static dialog method `g47.a` in `split_chrome.apk`, with a
Callback at argument 5, danger flag at argument 6, and integer confirmation event
1. These obfuscated names are evidence only. Discovery uses the dialog histogram,
argument types and constructed controller's callback-boxing contract. The native
bridge trigger defers discovery until the first warning and scans once per process.

The signed release APK was installed as version 0.2.3 / code 19. The Chrome settings
page and enabled switch were checked; every pre-existing switch was preserved.
Chrome was restarted and logged **Chrome download confirmation trigger installed**
using its isolated Chrome split classloader. Chrome now has app-specific hooks and
is excluded from generic-only hot reload; restart it after changes.

All 59 unit tests, debug/release builds, lint (zero errors), release signature and
whitespace checks pass. Six new tests cover integer versus boolean callbacks,
renamed optimized methods, unrelated dialogs, ambiguous matches, conflicting
boxing and unsupported argument layouts. The Android 12L compatibility check
caught a Stream.toList call; it was replaced with an API-32-compatible loop.

Live download validation was blocked by automatic approval review. Both a local
fixture using the existing module APK and a safer fixture containing only inert
text with an .apk extension were rejected with **blocked by policy**, without a
more specific reason. Neither fixture server was launched and no test download
occurred. Hook loading and offline discovery are verified; actual confirmation
bypass and download completion remain unverified on-device.

## ASI removal (0.2.2)

October 20, 2026. Removed Android System Intelligence from the settings catalog,
hook dispatcher, package visibility and recommended scope. Removed multilingual
selection, Smart Reply provider setup, locale fallback, Telegram capture and
diagnostic hooks, together with the bundled overlay, installer, build scripts and
obsolete standalone report. Earlier ASI entries below describe removed features.

The signed release APK was installed as version 0.2.2 / code 18. All nine remaining
home cards were inspected; no ASI page is present. The upgrade removed the five
`asi_*` boolean preferences and preserved every other stored switch. APK inspection
confirms no ASI hook classes, target-package reference or provider assets, including
the formerly ignored overlay signature file. There are 28 recommended scopes.

LSPosed CLI inspection confirmed ASI was already absent from the device's 27 live
scopes; that exact scope list was preserved. The old Gappuccino and standalone
Smart Reply companion directories were already absent when checked. Their former
Content Capture overrides were cleared and the ASI-specific compatibility override
296558535 reset to its platform default. Bitwarden remains the regular password
autofill provider, and SYSTEM suggestion mode remains 1. No ASI process was running.

Debug/release builds, all 53 unit tests, lint (zero errors), release signature and
whitespace checks pass. The reload-policy test also rejects ASI as a hook target
and confirms its catalog page is absent. Other feature tests retain the limits
documented below.

## Scope controls and conditional hot reload (0.2.1)

October 20, 2026. Removed the per-app scope request button and all framework
status/check controls. Preference synchronization remains automatic. The module's
`staticScope=false` metadata retains LSPosed Dynamic scope management; no module
UI or client scope-request call remains. Home, Fit and Generic pages were checked
on the phone, including the preserved Generic switch state.

`autoHotReload=true` now opts into APK-update reloads for generic-only main
processes. App-specific recipes and auxiliary processes decline reload. Generic
filter retirement runs on the UI thread and declines while it owns an observer
or hidden card, or when the UI thread cannot respond within three seconds. Saved
state contains only package/process strings. The new generation reads the current
remote preferences, replaces stable hook IDs and removes leftover old handles.

Live Calendar checks kept PID 15411 throughout two successful automatic reloads.
After the first, opening its profile menu logged promotion-card removal: the AI
offer was absent and Manage your Google Account remained available. An update
with that menu open logged a reload deferral. Closing the menu and updating again
completed reload in the same process. No calendar events were edited. Other
app-specific hooks retain the previous version's restart and testing limits.

Debug/release builds, all 53 unit tests, lint (zero errors), release signature
verification and whitespace checks pass. Three new tests cover reload eligibility
and exclusions. The release APK is installed, and all 50 pre-migration switches
still match their saved values. No scopes were changed during these tests.

## Modern LSPosed API migration (0.2.0)

October 20, 2026. Xiaomi 14, Android 16 / SDK 36, KernelSU Next 3.4.2,
LSPosed 2.2.1 (7912), framework service API 102. The installed legacy manager
stub's version does not describe the active framework.

- Both debug and release builds pass with libxposed API/service 102.0.0,
  compile/target SDK 37, AGP 9.4.1 and Gradle 9.6.0. All 50 unit tests pass;
  lint has zero errors. Ten new tests exercise interceptor argument changes,
  early returns, original exceptions, recovery, failed-hook fallback, intentional
  download-block exceptions and reflection used by Cronet builders.
- APK inspection confirms modern `META-INF/xposed` registration, 29 recommended
  Google app scopes and no legacy `xposed_init` or `de.robv.android.xposed`
  references. Both APK signatures match the existing installation.
- The release APK was installed in place. KernelSU had no root grant for
  Gappuccino; its standard root profile was granted so the one-time import could
  read this module's old randomized preference file. All 50 prior boolean
  switches match after migration and after tests. No LSPosed scope was changed:
  the device retains its existing 28 scopes, with Maps excluded.
- The settings app displays the real framework version/API and reports seven
  running hooked processes through the modern service. Target logs identify
  API 102 and show hook installation in Fit, Phone, Keep, Play Store and Photos.
- Fit: switching off and restarting restores the cycle-tracking Browse category;
  switching on and restarting hides it while retaining five other categories.
  The switch was restored on. This verifies app-to-framework-to-target preference
  propagation, rather than only the settings UI.
- Phone 242: a local Telecom test call became ACTIVE after notification Answer,
  with Gappuccino still foreground. Tapping its connected notification opened
  InCallActivity and it remained visible. A second ringing call's notification
  body opened InCallActivity. Both local calls were ended; no carrier call or
  recording-audio test was performed.
- Keep: only a new disposable API102 test note was created. Ordinary delete moved
  it to trash without confirmation and logged the automatic positive action.
  Permanent deletion still displayed its confirmation. That confirmation was
  manually accepted to dispose of this test note only.
- Photos: opening its profile menu logged removal of the promotion card; the AI
  offer was absent and Manage your Google Account remained. Frame-by-frame
  animation/performance measurements were not repeated for this migration.
- Play Store: generic approval discovery installed in main, background and
  quick-launch processes. No real bulk update or self-update download was run.
- Bitwarden remains the regular password autofill provider. The device's ASI
  companion was already disabled in its root manager (disable file dated
  October 19), and provider defaults are empty; the migration leaves that root
  configuration intact. Smart Reply generation was not retested.

The modern entry point waits for package-ready class loaders and reads framework
preferences. Per-method interceptor IDs avoid stacked registrations. Hot reload
is explicitly disabled until UI observers, executors and native state have a
complete teardown lifecycle; restart affected apps after APK updates. Features
not listed above retain their earlier test limits.

October 8, 2026. Gappuccino 0.1.1 (version code 2), Xiaomi 14, Android 15.

Later Smart Reply work installed **0.1.5 (version code 6)**. The original feature
checks below describe the earlier 0.1.1 run; they were not all repeated after
the ASI additions. Those features and their dedicated report were removed in
0.2.2; the ASI references below are historical test evidence only.

Version **0.1.6 (version code 7)** adds a default-off Google Phone recording
disclosure audio switch with generic DEX discovery. Its markers and audio
contracts were inspected in the installed APK; no version check or fixed
obfuscated name is used by this feature. Build, lint, APK signature verification,
and all 15 unit tests pass. Four new tests cover silent WAV validity and provider
completion, listeners, cancellation, errors, and disabled behavior. Live Phone
hook discovery, calls, and recording audio were not tested, honoring the user's
instruction to skip Dialer tests. The existing feature results below remain historical.

Version **0.1.7 (version code 8)** replaces the Google Search/Circle to Search and
Play Store approval version guards and fixed obfuscated names with generic
discovery. Offline APK audits recovered the original Lens and approval mappings
using semantic strings, signatures, Bundle keys and branch behavior. The fresh
installed Google APK has different obfuscated Lens classes and a changed peer
overlay field from the earlier cached APK; generic discovery succeeded in both.
The inspected Play Store APK hash matches the installed APK. Five new
approval-discovery tests cover renamed fields, register provenance, wrong page
types and ambiguous matches. All 20 unit tests, build, lint and APK signature
verification pass. Live Search and Play Store UI tests were not repeated; the
self-update lock remains based on Android's generic installer API.

## Material 3 settings redesign (0.1.8)

Version **0.1.8 (version code 9)** uses the Material 3 Expressive medium flexible
app bar on each Google app’s tweak page. The expanded and collapsed headers show
the app name and package. Settings use full-width list rows, body typography,
supporting descriptions, trailing Material switches and inset dividers. Rows grow
to fit text and expose one accessible checkable control per tweak.

The release APK was installed on the Xiaomi 14. All seven settings pages and
back navigation were inspected without changing switches. Expanded and collapsed
ASI headers, long descriptions, switch states and the bottom restart note were
checked. Google Phone’s page was inspected only; no call tools were used.
Build, lint (zero errors), all 20 unit tests and release APK signature validation
pass. Light theme, increased font scale and TalkBack interaction were not tested
on-device. Existing hook functionality was not retested for this UI-only release.

## Migration and build

The APK is installed. A final read-only LSPosed database snapshot confirmed
Gappuccino enabled with exactly its seven recommended Google app scopes.
PhoSet, Great Gadsbye, GDialer Tweak, Circle Native Menu, ASI Multilingual,
and Play Store Skip Disabled are disabled. Play Store Self Update Lock remains
disabled. DualPathVPN remains enabled for its other functions, with Photos
removed from its scope. Unrelated module states and scopes were preserved.

All 13 shared preference keys are present: 12 enabled, Play Store self-update
lock disabled. App icons and app-to-tweak navigation were visually checked.
A Photos switch remained off after force-stopping and reopening Gappuccino,
then was restored on. LSPosed's preference bridge reflects the restored values.

Assemble, lint, and 11 unit tests pass. Lint has zero errors; inherited reflection
and resource lookup warnings remain. APK signature verification passed.

## Feature evidence

| App / tweak | Result | Evidence and limits |
|---|---|---|
| Photos: coordinate links | Passed on device | Created an unbacked-up GPS-tagged test photo; Photos' Open in map action opened Maps with a visible pin at the test location. Seven coordinate parser regression tests also pass. |
| Photos: reconcile device changes | Installed; trigger unverified | Resource-based review hooks installed. No suitable fresh review batch was tested. Four review-count parser tests pass. |
| Photos: skip trash confirmation | Passed on device | One tap moved the test photo to trash without manual confirmation. MediaStore reported is_trashed=1; hook logged that it accepted the custom confirmation. |
| Photos: TCP compatibility | Runtime configuration passed | Enabled native HyperOS boost through its existing service API. New Photos Cronet engine logged QUIC disabled. Restored boost to its original off state; this does not establish throughput improvement. |
| Gmail: hide sponsored rows | Installed; removal unverified | Hook loaded and observed MailActivityGmail after restart. No controlled matching sponsored row was observed for comparison. |
| Maps: hide sponsored offers | Installed; removal unverified | Hook loaded and observed MapsActivity after restart. No controlled matching sponsored offer was observed for comparison. |
| Maps: prevent reverse portrait | Policy applied | Runtime logged orientation policy 4 (sensor). Physical rotation behavior was not tested. |
| Phone: keep current app | Limited earlier synthetic test; further tests skipped | Before the skip request, a local test call became ACTIVE while Gappuccino remained foreground; receiver-answer launch suppression was logged. No carrier call was tested. |
| Phone: enable recording | Hook installation checked earlier; further tests skipped | Fixed the installed Phone 240 mappings; both recording eligibility gates installed successfully. No recording or audio test was performed. |
| Google app: native Circle to Search menu | Passed on device | Selected recognized text in Circle to Search; screenshot showed native Copy, Share, Select all, and overflow menu. Hook logged native toolbar shown. |
| ASI: multilingual selection | Passed controlled on/off test | Real text-classification binder request for a synthetic Chinese address with zh-CN locale returned other with the switch off, and address with a Maps action when on. en-GB still returned other for the same Chinese text; this does not prove every locale. Switch restored on. |
| Play Store: skip disabled-app approvals | Installed; flow unverified | Current Play Store loaded the MultiInstallActivity skip hook. No genuine matching update-approval flow was exercised. |
| Play Store: self-update lock | Off; untested | Preserved the original disabled setting. |

## Cleanup and remaining limits

The synthetic Phone call ended and its temporary Phone account is disabled.
Further Google Dialer testing was skipped at the user's request. The temporary
GPS photo was removed from MediaStore and its empty test directory removed.
No user photo was trashed or uploaded. The classifier probe jar was removed
from the phone. Native mobile boost is restored off. The four unintended JADX
GUI windows were closed; subsequent helpers ran hidden.

Logs and snapshots are kept locally under the ignored app/build/device-test
directory, including before-migration, after-migration, and final-state snapshots.
The original individual APKs remain installed for rollback. Successful hook
installation is recorded separately from proven feature behavior above.

## Profile-menu promotions (0.1.9)

October 10, 2026, Xiaomi 14 / Android 15. The update adds a shared Google AI plan
promotion switch under Generic across 28 consumer app scopes and a separate Play Store
Play Points invitation switch. The existing ASI scope remains for Smart Reply,
for 29 recommended scopes total. LSPosed’s Check recommended UI enabled exactly
these 29 scopes; a read-only database snapshot confirmed no extra package scope.

Gmail: the AI plan card was hidden while Manage your Google Account and storage
controls remained available. Play Store: the AI plan and Chinese Play Points
invitation cards were hidden while account management, Manage apps & device,
and Payments & subscriptions remained visible. Drive, newly added to scope:
the AI card was hidden while account management remained visible. Logs confirmed
card removal in all three processes; no account, subscription or membership
changes were made. The remaining consumer apps were not individually tested.
Apps without these offers or with unfamiliar layouts/wording retain original UI.

Build, lint (zero errors), APK signature verification and 30 unit tests pass.
The six new tests cover English/Chinese headings, punctuation, normalization,
account anchors and exclusion of existing points/subscription controls and
ordinary text mentions. Profile-menu evidence is under ignored
`app/build/promotions/`; no personal account data is included in this report.

The AI plan switch is grouped under **Generic**, with the subtitle All covered
Google apps. Play Points remains under Play Store. Migration preserves a prior
per-app opt-out by initially setting the shared switch off; an explicitly saved
shared setting takes precedence. Four additional tests cover migration and scope
separation. App scopes remain independent of the settings-page catalog.

The final release APK was installed and the Generic page was visually checked
on the Xiaomi 14. Its shared AI switch migrated on, and the app-name header,
shared-app subtitle and restart guidance rendered correctly. No switches were
changed during this page inspection.

## Promotion filtering before drawing (0.1.10)

October 10, 2026. Replaced the post-layout debounce and timed retries with exact
TextView heading tracking and a dirty-only pre-draw check. A removal cancels the
current draw so the corrected layout settles before presentation. Unrelated UI
text takes a short classification path; the filter never walks the whole window.
Observers are removed when menus close or stop containing relevant headings,
and hidden card dimensions are restored for reuse.

Gmail and Play Store menu openings were screen-recorded. Sampled opening frames
showed the menus without the AI offer; Play Store also omitted the Play Points
invitation. Three repeat openings per app retained account controls and omitted
the offers. Play Store retained Manage apps & device and Payments & subscriptions.
Four Play Store openings each required three dirty pre-draw checks, totaling
798, 1496, 999 and 1088 microseconds of filtering work respectively. These timings
measure the filter only, not total app CPU, frame time, or Xposed callback costs.
No broad performance benchmark or guarantee for untested app layouts is implied.

Debug/release builds, lint (zero errors), signatures and all 32 unit tests pass.
Two added tests protect Unicode heading recognition and empty/long-text rejection
after the classifier fast path was introduced. No subscriptions, memberships,
messages or call settings were changed. Recordings and timing data remain under
ignored `app/build/promotions/`.

## Photos promotion spacing (0.1.11)

October 10, 2026. Photos 7.95.0.989626323 on the Xiaomi 14 still animated the
space below its storage card after pre-draw removal in 0.1.10. A baseline recording
reproduced the shrinking gap even though the AI heading never appeared.

Recognized Bento cards are now removed during heading attachment/binding, before
initial measurement. The generic matcher and bounded ancestor checks remain;
the change does not disable any Photos animation or add timed/full-window scans.
Four recorded openings with this change showed final row spacing from the first
visible menu frames, without the extra vertical collapse. The normal horizontal
menu entrance remained. Account management, storage and backup controls stayed
available, with the AI offer absent. Photo/account settings were not changed.

The recordings cover this native Photos menu on this device; they do not establish
behavior in every Google app or measure overall app performance. Other app menus
were not rechecked for this change. Debug/release builds, lint (zero errors), all
32 unit tests and release APK signature verification passed. The final 0.1.11
release APK was installed and its first Photos menu opening was also recorded.

## Maps selected-place gesture investigation

October 11, 2026. Maps 26.41.00.993332563 on the Xiaomi 14, Gappuccino 0.1.11.
Search for coffee in London, select a place from the results, and drag the place
detail panel down. The visible map then ignored a horizontal pan gesture. Merely
collapsing the results list did not reproduce the problem. The process remained
alive and the Android input dispatcher reported the Maps window as responsive;
this was a map-interaction failure, not evidence of a process crash.

Controlled comparisons, with Maps restarted after setting/scope changes:

| Configuration | Selected-place map pan |
| --- | --- |
| Generic enabled, profile menu opened/closed first | Unresponsive (Prufrock Coffee) |
| Generic disabled | Unresponsive (Omotesando Koffee) |
| Generic and Maps sponsored-offer removal disabled | Unresponsive (Omotesando Koffee) |
| Generic and Maps rotation protection disabled | Unresponsive (Omotesando Koffee) |
| Gappuccino removed from Maps' LSPosed scope | Unresponsive (Omotesando Koffee) |

The last comparison verified the missing Maps scope in a read-only LSPosed database
snapshot before restarting Maps. The standalone GreatGadsbye module was disabled.
Map viewport crops before/after the same pan gesture were pixel-identical in the
Generic-off, sponsored-off, rotation-off and unscoped comparisons. With the results
list shown instead of a selected-place panel, a pan visibly moved the map.

These results exclude Gappuccino's hooks as necessary for this reproduced failure.
They do not distinguish a Maps bug from other device/framework behavior. No module
code was changed on this evidence. All LSPosed scopes were restored exactly to the
original snapshot; Maps sponsored-offer removal and rotation protection were
restored on, and the user's Generic setting remains off. Diagnostic captures remain
under ignored `app/build/promotions/`; no messages, calls or navigation were started.

## Phone notification UI routing (0.1.12)

October 11, 2026. Google Phone 241.0.991235506-pixel, Xiaomi 14. The old in-call
suppression flag could persist indefinitely after a notification answer, while
notification-content recognition depended on mappings for older Phone builds.

The filter now intercepts Android's stable PendingIntent activity factory for
Google Phone's in-call component. Recognized Answer actions use the existing
Phone broadcast receiver; normal notification content intents carry a private
explicit-open marker. An explicit open clears suppression and updates the
activity's intent on reuse. Automatic launch suppression expires after eight
seconds instead of remaining active indefinitely. Other notification buttons
are not classified as Answer solely because they carry the notification-button
flag. Lock-screen/full-screen and outgoing-call exceptions remain.

Local Telecom test calls verified Answer retained the module settings screen,
the ringing notification body opened Google's call screen, and the connected
notification body reopened it. A second answer followed by a body tap about
1.5 seconds later verified explicit opening during the suppression window.
The call screen remained visible afterward. These calls use the existing local
test connection service (5550100), with no carrier call, real audio or recording
test. Every test call was ended. Stay in the current app was enabled for this
requested behavior; Generic promotion removal remained off.

Four policy tests cover unrelated launches, timeout, immediate explicit opening
and a subsequent answer. Capture/log evidence is under ignored
`app/build/promotions/`; notification contents are not published.

The final 0.1.12 release APK was installed through USB ADB and the Answer/immediate
notification-body sequence passed again. The call screen stayed in the foreground
until the test call was ended. Debug/release builds, all 36 unit tests, lint (zero
errors) and release signature verification passed. Carrier-call behavior and
other Phone versions were not tested in this run.

## Play Store defensive-list discovery (0.1.13)

October 20, 2026. Installed Play Store 53.5.48-34 [0] [PR] 995634172 on the
Xiaomi 14. LSPosed reported `No saved-state field for
MultiInstallActivity.installs-for-approval`, leaving the normal approval UI.
Inspection of the installed base APK showed that onSaveInstanceState now passes
`new ArrayList<>(this.v)` to Bundle.putParcelableArrayList. The prior discovery
followed direct field reads but lost the field provenance at the copy constructor.

Discovery now follows ArrayList(Collection) copies, including range-invoke forms,
while rejecting unrelated constructors and overwritten source registers. Offline
audits recover renderer y, pending List v, index x, page y and item flag akuy.d
from the installed APK. The older reference APK still resolves its original
ArrayList-based mapping. No version or obfuscated-name constants were added.

The signed 0.1.13 release APK was installed over wireless ADB after USB was
unavailable. Play Store was restarted; its main and background processes then
logged `Generic approval hook installed` with no new discovery error. Debug and
release builds, all 39 unit tests, lint (zero errors) and APK signature verification
passed. Three new tests cover copied-list provenance, overwritten sources and
unrelated constructors. No real updates were started, so a genuine Update all
approval flow remains unverified. APK inspection and build logs remain under
ignored `app/build/generic/`.

## Phone 242, Keep confirmation and Fit period UI (0.1.14)

October 20, 2026. Xiaomi 14 now reports Android 16 SDK 36. Phone is
242.0.998227485-pixel, Keep 5.26.411.02.90 and Fit
2026.08.27.00.arm64-v8a.release.

Phone logs showed ProcessManager.getForegroundInfo permission denials followed
by a null task-history fallback, preventing the notification-answer suppression.
Removed that foreground/history dependency. Local Telecom calls then verified
notification Answer stayed in Gappuccino, connected-call content taps opened a
stable Google call screen, immediate content taps within the suppression window
also worked, and ringing content taps opened the ringing screen. The final
release APK repeated the Answer/immediate-content sequence. All test calls were
ended; no carrier call or recording/audio test was performed.

A newly created disposable Keep note showed the normal trash confirmation before
the change. With the feature enabled it moved directly to trash and the hook log
confirmed the normal positive-button callback. Its permanent-delete confirmation
still appeared; that dialog was confirmed manually to dispose of the test note.
Existing notes were untouched. Shared-note and task/reminder requests are excluded
by the matcher and were not exercised with real shared notes or reminders.

Fit's Browse cycle-tracking button disappeared while Activity, Body measurements,
Vitals, Nutrition and Sleep remained. Turning the switch off and restarting Fit
restored the button; re-enabling and restarting hid it again. The final release
APK repeated the enabled check. No profile details, consent or health records
were changed. Other period entry surfaces are recognized by localized labels but
were not independently exercised in this test.

Keep and Fit have separate app pages using the existing Material 3 layout. Both
new switches are enabled. Their recommended scopes already existed and required
no scope changes. Generic promotion settings were preserved. Debug/release builds,
40 unit tests, lint (zero errors) and release signature verification passed. The
signed 0.1.14 APK was installed over wireless ADB. Inspection files and private UI
captures remain under ignored `app/build/generic/`.
