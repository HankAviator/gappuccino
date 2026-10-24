# Gappuccino

An LSPosed module for Google app tweaks and profile-menu promotion removal.
Tap an app icon to configure its tweaks. Material 3 medium flexible headers show
each app’s name and package, with accessible list switches and Android dynamic colors.

## Features

| App | Features |
|---|---|
| Google app profile menus | Hide Google AI plan offers in compatible account-menu cards; one shared switch under Generic |
| Google Chrome | Automatically accept the “File might be harmful” download confirmation for APKs and other file types |
| Google Photos | Open photo coordinates in maps; reconcile device changes; skip trash confirmation; use TCP during HyperOS mobile boost |
| Gmail | Hide sponsored emails in Promotions and Social |
| Google Keep | Skip ordinary note move-to-trash confirmations; retain shared-note, permanent-delete and reminder choices |
| Google Fit | Hide period and cycle-tracking entries without changing profile or health data |
| Google Maps | Hide sponsored offers; prevent upside-down portrait |
| Google Phone | Answer from notifications without leaving the current app; enable call recording; optionally silence recording disclosures |
| Google Search | Native Circle to Search text-selection toolbar with smart actions |
| Google Play Store | Hide the Play Points invitation; skip approval screens for apps with auto-update disabled; block Play Store self-updates |

## Setup

Requires **Android 12L or later**, root, and an LSPosed-compatible framework
implementing **libxposed API 102 or later**.

1. Install the APK, enable the module in LSPosed and select its recommended Google app scopes.
2. Open Gappuccino to review the switches.
3. Disable overlapping tweaks from other modules.
4. Restart the affected apps after changing settings, or reboot.

The recommended scope includes Google consumer apps (including Chrome, Drive, Docs,
YouTube, Gemini, Chat, Keep and Gboard).
Manage scope in LSPosed using **Dynamic scope management** and its recommended apps.
No System Framework or Xiaomi app scope is required.

The settings app uses the modern LSPosed service to publish preferences. Target apps receive
read-only preferences. When upgrading from the legacy module, Gappuccino imports
its previous switches once using root; keep the existing installation and its data.
Grant Gappuccino root access in your root manager before importing older settings.
APK updates can hot reload the Generic filter in apps with no app-specific tweaks,
such as Calendar, Drive and Translate, when their profile menu is closed. Apps with
their own tweak page, active profile menus or a busy UI thread require an app restart.
Restart affected apps after changing switches.

### Profile-menu promotions

**Generic → Hide Google AI plan promotion** controls all covered Google consumer
apps. Play Store also
has **Hide Play Points invitation**. These hide offers, without changing subscriptions
or account membership. The current account-menu filter recognizes the English AI
plan heading and English/Chinese Play Points invitation headings. Ordinary mentions
and existing points controls remain available. Recognized cards are filtered before
drawing; recognized Bento cards are removed during binding/attachment to avoid a
collapsing gap when the menu opens. No delayed retries or full-window scans are used.
Apps without a compatible card,
including unfamiliar Compose layouts or offer wording, retain their original UI.

## Compatibility

- Chrome's switch skips the file-type confirmation through Chrome's own acceptance callback. Safe Browsing scans, malware-block recovery, enterprise-policy warnings and other download dialogs retain their original behavior. Restart Chrome after changing the switch.
- Google Search, Play Store approval skipping, and Phone disclosure suppression discover compatible implementations automatically. Unsupported implementations retain their original behavior.
- Phone notification routing uses stable Android pending-intent factories: Answer can stay in the current app, while tapping the notification opens the call screen. Recording eligibility currently supports `236.0.969488611-pixel` and `240.0.986973448-pixel`. HyperOS's separate call UI may still appear.
- Recording disclosure suppression defaults off; actual call audio has not been verified.
- Photos TCP compatibility applies only to new connections while HyperOS mobile boost is enabled. Photos backup restrictions still apply.

## Build

Requires JDK 17+, Android SDK platform 37.0 and Build Tools 37.0.0.
The wrapper uses Gradle 9.6.0 with Android Gradle Plugin 9.4.1.

```powershell
./gradlew.bat --no-daemon :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Release builds currently use the local Android debug signing key.

See [device test results](DEVICE-TEST-RESULTS.md) and [hook sources](SOURCES.md) for technical evidence and limitations.
