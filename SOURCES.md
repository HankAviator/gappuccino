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
| ASI Multilingual | `D:/codex/2026-10-07/d/work/asi-multilingual` | Existing classifier fallback, with a startup switch |
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

Smart Reply provider resources and initial setup requirements came from
`D:/codex/2026-10-07/goo/work/smart-reply`. The consolidated APK now owns their
replacement under `providers/` and `app/src/main/assets/providers/`; the older
Magisk provider setup is disabled. The locale and diagnostic mappings were
checked against the actual installed ASI V41 APK, whose SHA-256 matched the
previously decompiled `D:/codex/2026-10-07/d/work/asi-update/android15-oem.apk`:
`8c9013114bb336f35907bb41b276e57614e62c2e94d3f1f79d74d93b274777d8`.
Version-specific mappings are guarded to that ASI version name.

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
