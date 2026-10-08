# Custom Xposed module comparison

Inspected 2026-10-08 on Xiaomi 14 (houji), Android 15, via USB ADB.

Evidence: installed package metadata, a local snapshot of LSPosed configuration including its WAL, local project files, and targeted searches of local Codex conversation logs. Enabled means configured enabled in LSPosed; this does not prove each hook currently works.

16 custom modules installed: 14 enabled, 2 disabled. No phone settings, packages, or module states were changed.

| Module | Installed version | LSPosed | Current scope | Source |
|---|---|---|---|---|
| DualPathVPN | 1.0.19 | Enabled | `com.android.settings`, `com.github.dyhkwong.sagernet`, `com.google.android.apps.photos`, `system` | `D:\myplace\modules\DualPathVPN` |
| Fixed Calendar Color | 1.0 | Enabled | `com.android.providers.calendar` | Not located yet |
| PhoSet | 0.1.4 | Enabled | `com.google.android.apps.photos` | `D:\myplace\modules\phoset` |
| Great Gadsbye | 1.1.0 | Enabled | `com.google.android.apps.maps`, `com.google.android.gm` | `D:\myplace\modules\great-gadsbye` |
| GDialer Tweak | 0.11.0 | Enabled | `com.android.incallui`, `com.google.android.dialer` | `D:\myplace\modules\gdialer-tweak` |
| Play Store Self Update Lock | 1.1 | Disabled | `com.android.vending` | `D:\codex\2026-09-15\hello-so-recently-i-noticed-my\work\PlayStoreSelfUpdateLock` |
| Rotation Guard | 1.1 | Enabled | `system` | `D:\codex\2026-09-20\the\work\rotation-fix` |
| QR Warning Skipper | 1.2 | Enabled | `com.xiaomi.scanner` | `C:\Users\Han\Documents\Codex\2026-08-02\connect-to-my-phone-192-168\work\qr-warning-skipper` |
| App Locale Persistence Fix | 1.0 | Enabled | `system` | `D:\codex\2026-09-29\aerl\work\AppLocalePersistenceFix` |
| Alarm Fade | 1.0 | Enabled | `com.android.deskclock` | `D:\codex\2026-10-02\is-i\work\alarm-fade` |
| Privacy Delay | 1.1 | Disabled | `com.android.systemui` | `D:\codex\2026-09-12\after-i-rooted-my-phone-the\work\privacy-delay` |
| eSIM Bypass | 1.1 | Enabled | `com.miui.euicc` | `C:\Users\Han\Documents\Codex\2026-08-02\w\outputs\EsimBypass-1.1-source.zip` |
| PowerKeeper Battery Guard | 1.0 | Enabled | `com.miui.powerkeeper` | `D:\codex\2026-09-19\my\work\powerkeeper-battery-guard` |
| Circle to Search Native Menu | 1.3 | Enabled | `com.google.android.googlequicksearchbox` | `D:\codex\2026-10-07\goo\work\cts-native` |
| ASI Multilingual | 1.0 | Enabled | `com.google.android.as` | `D:\codex\2026-10-07\d\work\asi-multilingual` |
| Play Store Skip Disabled | 1.0 | Enabled | `com.android.vending` | `D:\codex\2026-10-08\dis\work\module` |

## Comparison findings

- DualPathVPN 1.0.19, PhoSet 0.1.4, Great Gadsbye 1.1.0, and GDialer Tweak 0.11.0 have local project version numbers matching the installed packages. Source-to-APK equivalence has not yet been checked.
- Installed Rotation Guard is 1.1; the nearby APK filename says v1.0. Installed QR Warning Skipper is 1.2; its nearby APK filename says v1.1. Installed Circle to Search Native Menu is 1.3. Filenames alone are not authoritative version evidence.
- Privacy Delay 1.1 and Play Store Self Update Lock 1.1 are installed but disabled. Preserve disabled state when migrating.
- LSPosed retains uninstalled, disabled entries for dev.codex.qrwarningskipper, com.codex.esimbypass, and io.github.hankaviator.vendingcronetoff. These are historical entries, not active installed modules.
- Fixed Calendar Color source has not yet been located. The installed APK is available on the phone for inspection.
- gappuccino currently contains only README.md, LICENSE, and Git metadata.

## Consolidation approach

Use one package under io.github.hankaviator with independent feature switches, per-package hook dispatch, and a recommended scope covering the selected features. Keep the original module states and feature preferences as the migration baseline. Review hooks sharing Photos and System Framework to avoid duplicated interception. Recover and inspect current source versions before merging. Disable corresponding originals during activation of the replacement to avoid running both implementations.

The user selected Google apps only, with app-icon buttons on the home screen and individual tweaks under each app. Gappuccino includes seven Google apps and 13 switches; see README.md. Xiaomi apps, System Framework, the calendar provider, and VPN apps are excluded. Only DualPathVPN's Photos transport hook is included. Third-party modules are not included in this comparison. Local Codex logs are available; separate cloud ChatGPT conversations have not been accessed.
