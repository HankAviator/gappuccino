# Device test results

October 8, 2026. Gappuccino 0.1.1 (version code 2), Xiaomi 14, Android 15.

Later Smart Reply work installed **0.1.5 (version code 6)**. The original feature
checks below describe the earlier 0.1.1 run; they were not all repeated after
the ASI additions. See [SMART-REPLY-RESULTS.md](SMART-REPLY-RESULTS.md) for the
new implementation, live-state checks, WhatsApp result, and Telegram limit.

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
