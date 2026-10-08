# Gappuccino

An LSPosed module with **18 independent tweaks across seven Google apps**.
Tap an app icon to configure its tweaks. Material 3 medium flexible headers show
each app’s name and package, with accessible list switches and Android dynamic colors.

## Features

| App | Features |
|---|---|
| Google Photos | Open photo coordinates in maps; reconcile device changes; skip trash confirmation; use TCP during HyperOS mobile boost |
| Gmail | Hide sponsored emails in Promotions and Social |
| Google Maps | Hide sponsored offers; prevent upside-down portrait |
| Google Phone | Answer from notifications without leaving the current app; enable call recording; optionally silence recording disclosures |
| Google Search | Native Circle to Search text-selection toolbar with smart actions |
| Android System Intelligence | Multilingual smart selection; Smart Reply provider setup; reply-language fallback; optional diagnostics and experimental Telegram capture |
| Google Play Store | Skip approval screens for apps with auto-update disabled; block Play Store self-updates |

## Setup

Requires **Android 12L or later**, root, and LSPosed. Smart Reply provider setup also requires Magisk.

1. Install the APK and open Gappuccino to review the switches.
2. Enable the module in LSPosed and select its recommended Google app scopes.
3. Disable overlapping tweaks from other modules.
4. Restart the affected apps after changing settings, or reboot.

The recommended scope contains only the seven Google apps above. No System Framework or Xiaomi app scope is required.

### Smart Reply

Enable **Smart Reply providers** under Android System Intelligence, then reboot.
Gappuccino installs its Magisk support files and preserves your regular password autofill provider.
Reboot after disabling the providers to remove their service defaults.

Suggestions depend on the messaging app, conversation language, and available models.
WhatsApp is confirmed working. Official Telegram does not currently show suggested replies;
its experimental capture switch is for diagnostics only. The display-language fallback
uses English models for supported conversations and does not add Chinese reply models.

## Compatibility

- Google Search, Play Store approval skipping, and Phone disclosure suppression discover compatible implementations automatically. Unsupported implementations retain their original behavior.
- Phone recording eligibility and notification routing currently support `236.0.969488611-pixel` and `240.0.986973448-pixel`. HyperOS's separate call UI may still appear.
- Recording disclosure suppression defaults off; actual call audio has not been verified.
- Smart Reply language fallback and experimental Telegram capture currently support ASI V41.
- Photos TCP compatibility applies only to new connections while HyperOS mobile boost is enabled. Photos backup restrictions still apply.

## Build

Requires JDK 17+ and Android SDK platform 35.

```powershell
./gradlew.bat --no-daemon :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Run `./providers/build.ps1` when changing the embedded Smart Reply overlay resources.
Release builds currently use the local Android debug signing key.

See [device test results](DEVICE-TEST-RESULTS.md), [Smart Reply details](SMART-REPLY-RESULTS.md),
and [hook sources](SOURCES.md) for technical evidence and limitations.
