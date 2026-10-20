# Hook provenance

The unified settings app, catalog, and allowlisted dispatcher are new. Hook
implementations retain their original Java packages for traceability; there is
only one APK and one Xposed entry point. Preference readers now use
`io.github.hankaviator.gappuccino/features`. No original repositories were edited.

| Source | Location | Integration |
|---|---|---|
| PhoSet 0.1.4 | `../phoset` | All three existing Photos fixes; unified preference owner and BuildConfig |
| Great Gadsbye 1.1.0 | `../great-gadsbye` | Gmail/Maps ad removal and Maps rotation; unified preference owner |
| GDialer Tweak 0.11.0 | `../gdialer-tweak` | Google Phone hooks and local test-call tools; Xiaomi InCallUI hooks removed; recording default matches the inspected phone |
| DualPathVPN 1.0.19 | `../DualPathVPN` | Only PhotosTransportHook; gated by its own switch and the native boost setting |
| Circle Native Menu | `D:/codex/2026-10-07/goo/work/cts-native` | Existing toolbar behavior; generic Lens discovery replaces the original fixed mapping |
| Play Store Skip Disabled | `D:/codex/2026-10-08/dis/work/module` | Existing skip behavior; generic approval-state discovery replaces the original fixed mapping |
| Play Store Self Update Lock 1.1 | `D:/codex/2026-09-15/hello-so-recently-i-noticed-my/work/PlayStoreSelfUpdateLock` | Existing session rejection hook, disabled by default |

The consolidated Phone hook also adds mappings recovered from the phone's actual
240.0.986973448-pixel APK. Recording gates are `ldt.a` and `lcy.a`; notification
PendingIntent routing uses `rve` and `ahdi.b`. Original 236 mappings remain
available. Mapping selection occurs after Application.attach, using the installed
package version; unknown versions skip those obfuscated hooks.

Recording disclosure suppression uses generic semantic DEX discovery with
[DexKit](https://luckypray.org/DexKit/en/guide/quick-start), pinned to 2.3.0. Unlike
the existing Phone mappings, it does not check version numbers or fixed obfuscated
names. The implementation was inspected against the installed Phone APK, SHA-256
`a68460773d707413ed9194a6ecbdf6c719feafb878379c527cd185296550f1c5`.
Its `CallRecordingDisclosure(startingAudio=` and `RawInjectableAudio(audioFile=`
markers identify the payload and raw-file audio classes. The semantic
`com/android/dialer/callrecording/disclosure/impl/TtsCallRecordingDisclosure`
marker identifies the legacy provider contract; all implementing recording
providers are discovered through that interface. Constructor and Future signatures
are verified before hooking. Original state/completion code continues to run.

Circle to Search now uses DexKit to identify the Lens peer by its overflow-menu
and clipboard error strings. Its view/text accessors are selected by signatures,
and the select-all action by its semantic diagnostic string. The shared overlay
field relationship is checked through that action's field references. No fixed
obfuscated names or Google app version checks remain in this hook.

Play Store approval discovery uses [dexlib2](https://github.com/JesusFreke/smali),
pinned to 2.5.2, to associate `Bundle` saved-state keys with the pending list,
current index and page fields. The approval item's boolean is identified by the
true branch that sets the auto-update-disabled page type. This preserves the
original skip-before-render behavior without fixed obfuscated names or a version
guard. The public MultiInstallActivity component and semantic Bundle keys remain
required. Missing or ambiguous matches retain the original approval flow.

Play Store 53.5.48 saves a defensive `new ArrayList<>(pendingList)` rather than
the pending field itself. In 0.1.13, saved-state discovery follows the standard
ArrayList(Collection) constructor back to the original field, including DEX
range-invoke forms. Other constructors are not treated as copies. Offline audits
resolve both this build and the prior reference APK without fixed field names.
The inspected 53.5.48 base APK SHA-256 is
`99e272eb43df794fb96c5c60ca5dfc0b7b96b16465e120b00eda374c559ea272`.

Offline audits checked both the earlier cached Google APK and a fresh pull of the
installed Google APK. Their obfuscated classes differ: the generic rules found
`drac`/`dutp.ad` in the earlier APK and `drti`/`dvmz.ag` in the installed APK,
including the changed peer overlay field. These names are evidence only; the hook
does not contain them. The installed Google APK SHA-256 is
`3a20044f40f1b85f788a70674081fb9f316028d40e5faa799fcbc33a5e983c03`.
The Play Store audit used `D:/codex/2026-10-08/dis/work/playstore.apk`, whose hash
matched the installed APK:
`18c499981131e49964775271f9749a2fa1a5580550672a16215146290fe00f5c`.
This verifies the inspected implementations; future structural changes can still
require revised discovery rules.

## Initial settings evidence

Read-only inspection of the existing LSPosed shared preference files confirmed:

- PhoSet: coordinate fix and reconciliation enabled; trash-confirmation preference
  absent, so its existing implementation defaults to enabled.
- Great Gadsbye: Gmail, Maps, and Maps rotation enabled.
- GDialer Tweak: keep-current-app and recording enabled.
- LSPosed configuration: Play Store Self Update Lock disabled; the other included
  original modules enabled.

Old preference keys `hide_order_photos` and `maps_review_composer` are unused by
the current implementations. Targeted conversation-log inspection confirmed the
Maps review feature was explicitly reverted. They are not migrated.

## Phone notification routing

In 0.1.14, answering no longer depends on HyperOS ProcessManager foreground
information or restricted ActivityManager task history. Android 16 on the test
phone denies that access. The notification's Answer action supplies the routing
signal, while keyguard/full-screen and explicit-open exceptions remain.

Phone notification routing in 0.1.12 no longer uses the old obfuscated pending-intent
mappings. It hooks Android PendingIntent.getActivity for the stable Google Phone
in-call component, distinguishes Answer actions from notification content, and
uses a private marker for explicit openings. The answer-transition state expires
and is cleared on explicit launch, including activity reuse through onNewIntent.
Local Telecom calls validated this against Phone 241.0.991235506-pixel.

## Modern LSPosed runtime

The runtime uses [libxposed API 102](https://github.com/libxposed/api) and
[service 102](https://github.com/libxposed/service). Registration is in
`META-INF/xposed`; no legacy Xposed entry point or framework classes are bundled.
Package-ready callbacks install hooks against the app's final class loader.
Interceptor chains preserve each recipe's before/after behavior and original
exceptions. Stable per-interceptor IDs prevent duplicate registrations on the
same method. The Play Store self-update block explicitly uses passthrough
exception mode so its intended IOException reaches the downloader.

The settings app publishes private preferences through the framework service.
Target processes use read-only remote preferences. A one-time root import reads
only this module's legacy randomized preference directory. Scope management is
left to LSPosed's Dynamic scope management UI. Automatic hot reload is limited to
generic-only main processes, derived from the covered package allowlist and absence
of an app-specific tweak page. Retirement checks run on the main thread; any
profile observer or hidden card vetoes reload. Otherwise the old filter stops
accepting work and clears its markers. Only package/process strings are handed to
the new generation, which reinstalls the filter using stable hook IDs and removes
old handles. App-specific hooks, auxiliary processes and busy UI threads retain
their restart requirement. No native libraries or workers are loaded by this
supported reload path.

## Keep confirmation and Fit period entries

Keep confirmation discovery uses dexlib2 to locate the dialog-fragment argument
contract (`_requestCode`, `_positiveTextId`, `_displayOptions`) and its Bundle field.
Only `request_trash_note` and `confirm_trash_notes` dialogs invoke the app's normal
positive button automatically. The callback retains Keep's own trash/undo behavior.
Shared-note requests, permanent deletion, label deletion and reminder choices are
not matched. This was inspected and tested against Keep 5.26.411.02.90; unknown
contracts retain the original dialog.

Fit's native entries are recognized by localized resource values for period,
cycle tracking, add-period and menstruation labels. Matching buttons or their
nearest clickable row are hidden during text binding/attachment, with at most
eight ancestor checks and no full-window scans. Dimensions are restored for
detached or rebound views. This changes UI only, independent of profile gender;
profile details, consent and health records are untouched. Fit
2026.08.27.00.arm64-v8a.release was inspected and its Browse category tested.

## Profile-menu promotion filtering

The generic filter is based on the live Google account menu inspected on October
10, 2026. Gmail exposes `og_bento_card_title`, a single-item RecyclerView stack and
a CardView wrapper. Play Store uses the same hierarchy with obfuscated resource
IDs; its clickable Manage your Google Account control supplies the menu anchor.
Exact offer headings identify the AI plan and Play Points invitation, rather than
obfuscated classes or app versions. Unknown menu layouts retain original behavior.

The 0.1.10 filter observes native TextView attachment/binding and checks cached
heading markers at pre-draw, rather than scanning Activity/Dialog trees after
a delay. First-opening recordings and repeated menu checks cover Gmail and
Play Store on the Xiaomi 14. Native Android pre-draw cancellation keeps layout
edits out of the presented frame.

The 0.1.11 filter additionally removes recognized Bento cards during native heading
attachment or binding, before initial measurement. Photos' account menu otherwise
animates the gap left by a card removed at pre-draw. This path uses the same bounded
ancestor checks and generic heading matching; no Photos version or obfuscated class
is targeted, and no app animations are disabled. Pre-draw remains a fallback.
